# Expedited Orders Review

An internal tool for the operations team. An agent reviews customer orders and approves the eligible
ones for expedited processing.

| Project | Stack | Path |
|---|---|---|
| Orders API | Java 21, Spring Boot 3.5, REST, in-memory data | [`backend/`](backend) |
| Review UI | React 19, TypeScript, Vite | [`frontend/`](frontend) |

## Run it

```bash
# 1) Backend on http://localhost:8080 (seeds 13 sample orders on startup)
cd backend
./mvnw spring-boot:run          # or: mvn spring-boot:run
./mvnw test                     # 44 tests: rules, engine, approval/concurrency, HTTP + CORS

# 2) Frontend on http://localhost:5173
cd frontend
npm install
npm run dev
npm test                        # component tests
npm run build                   # type-check + production build
```

The seed data uses timestamps relative to startup. After 10 minutes every inventory check is stale,
so every order becomes "not eligible", which is the correct result. Use **Re-check inventory
(simulated)** in the UI, or `POST /api/orders/{id}/inventory/refresh`, to record a fresh check.

## Step-by-step checklist

Each step lists the requirement and where it is implemented, so it can be checked off.

### Step 1: Backend structure (separate evaluation logic, rule definitions and API)

- [x] **Domain** (`domain/`): immutable records (`Order`, `Customer`, `Shipping`, `InventoryStatus`, ...). Raw data is kept as received.
- [x] **Rule definitions** (`eligibility/rule/{hard,score,quality}/`): one class per rule.
- [x] **Evaluation logic** (`eligibility/EligibilityEngine`): runs the rules and combines their results. It knows no individual rule.
- [x] **Application service** (`service/OrderService`): reads orders, evaluates them and handles approval.
- [x] **API layer** (`api/`): controller, DTOs and exception handler only. It contains no business logic.
- [x] **In-memory data** (`repository/InMemoryOrderRepository`, a `ConcurrentHashMap`), seeded by `SampleDataLoader`.

### Step 2: Rule system (adding a rule does not modify existing rules)

- [x] `EligibilityRule` is the extension point. Every Spring bean implementing it is injected into the engine as `List<EligibilityRule>`.
- [x] Convenience interfaces: `HardRule` (returns a blocking reason) and `ScoringRule` (fixed penalty).
- [x] **To add a rule**, create one new class:

  ```java
  @Component
  public class PoBoxShippingRule implements HardRule {
      public String id() { return "PO_BOX_SHIPPING"; }
      public Optional<String> violation(EvaluationContext ctx) { ... }
  }
  ```
  You do not change the engine, the other rules, the API or the UI. The test `newRulesPlugInWithoutTouchingExistingOnes` proves it.
- [x] Thresholds and limits live in `application.yml` under `eligibility.*` (`EligibilityProperties`).

### Step 3: Hard rules (any failure means NOT eligible)

| Rule | Class |
|---|---|
| `inventoryStatus.available = false` (or inventory missing) | `InventoryAvailableRule` |
| `lastCheckedAt` older than 10 min (or null, which means never checked) | `InventoryFreshnessRule` |
| `customer.tier` is null (or customer missing) | `CustomerTierRule` |
| `items` is empty (or null) | `ItemsPresentRule` |
| *Carried over, assumed:* unsupported or missing shipping country | `ShippingDestinationRule` |
| *Carried over, assumed:* blocking risk flag (`FRAUD_SUSPECTED`, `CHARGEBACK_HISTORY`) | `BlockingRiskFlagRule` |
| *Carried over, assumed:* order status `REJECTED` | `OrderStatusRule` |

### Step 4: Scoring (start at 100, never below 0)

| Deduction | Class |
|---|---|
| -30 total quantity > 3 | `HighQuantityRule` |
| -20 amount > tier threshold | `TierAmountThresholdRule` |
| -15 `addressValidated = false` | `AddressNotValidatedRule` |
| -25 `riskFlags` contains `HIGH_VALUE` | `HighValueRiskFlagRule` |
| -10 `createdAt` older than 48 h | `OrderAgeRule` |

- [x] Final rule: `eligible = no hard rule failed AND score >= 60` (`EligibilityEngine`). A score below 60 adds the blocking reason `MINIMUM_SCORE`.
- [x] Boundaries: 10 min and 48 h are inclusive (exactly 10 min old is still fresh), and a score of exactly 60 is eligible.

### Step 5: Missing or inconsistent data (decisions)

| Situation | Decision | Rule |
|---|---|---|
| `totalAmount` differs from the sum of the items | Warning. The **higher** value is used for the tier threshold, so the order value is never under-estimated. | `TotalAmountConsistencyRule` |
| `totalAmount` missing | Warning. The items total is used. | `TotalAmountConsistencyRule` |
| `riskFlags` missing (null) | Treated as empty, with the warning "risk assessment missing". | `MissingRiskAssessmentRule` |
| `addressValidated = null` | Warning only. The -15 penalty applies to `false`, not to "unknown". | `AddressValidationPendingRule` |
| Item quantity missing or <= 0, or price missing | Warning. The item is ignored in totals. | `ItemDataQualityRule` |
| `createdAt` missing or in the future, `lastCheckedAt` in the future | Warning | `TimestampSanityRule` |
| `inventoryStatus.lastCheckedAt = null` | **Blocking**, because availability cannot be trusted | `InventoryFreshnessRule` |
| `shipping` or `customer` missing | **Blocking** | `ShippingDestinationRule` / `CustomerTierRule` |

### Step 6: Evaluation response

`GET /api/orders/{id}/eligibility`

```json
{
  "eligible": true,
  "score": 70,
  "reasons": ["Total quantity 4 exceeds 3 (-30)"],
  "warnings": [],
  "decision": "ELIGIBLE_WITH_RISK",
  "blockingReasons": [],
  "findings": [{ "ruleId": "HIGH_QUANTITY", "severity": "PENALTY", "message": "...", "penalty": 30 }],
  "evaluatedAt": "2026-10-15T12:00:00Z"
}
```

- [x] The required fields (`eligible`, `score`, `reasons`, `warnings`) appear exactly as specified.
- [x] `reasons` lists the blocking reasons first, then the score deductions. `warnings` holds the data-quality notes.
- [x] Additional fields let the UI show the result without recomputing it:
  - `decision`: `ELIGIBLE`, `ELIGIBLE_WITH_RISK` or `NOT_ELIGIBLE`
  - `blockingReasons`
  - `findings`: each finding carries its severity
- [x] "Eligible but risky" means eligible with a score below 80 (`eligibility.risky-score-below`) or with at least one warning.
- [x] Eligibility is computed on every read, because the time-based rules change the result over time.

### Step 7: Approval endpoint

`POST /api/orders/{id}/approve-expedited`

| Case | Response |
|---|---|
| Eligible | `200 {"status":"APPROVED","alreadyApproved":false,"approval":{...},"eligibility":{...}}` |
| Already approved (repeat call) | `200 {"alreadyApproved":true, ...}`. Idempotent: no change and no re-evaluation, and the original approval snapshot is returned. |
| Became ineligible (re-evaluated at approval time) | `422 {"code":"NOT_ELIGIBLE","reasons":[...],"warnings":[...],"eligibility":{...}}` |
| Status `REJECTED` | `409 {"code":"INVALID_STATUS", ...}` |
| Unknown id | `404 {"code":"ORDER_NOT_FOUND", ...}` |

- [x] **Concurrency (simplified):** a per-order lock in `OrderService` covers load, evaluate and save. Exactly one request performs the transition; the others see `alreadyApproved: true`. The test `concurrentApprovalsTransitionExactlyOnce` runs 32 threads against one order.

### Step 8: Frontend

- [x] Shows eligibility, score, reasons and warnings (`EligibilityPanel`).
- [x] Separates blocking issues from other findings: red for blocking issues, amber for score deductions, yellow for warnings. The grouping uses the severity assigned by the backend.
- [x] Clear difference between the three outcomes. Each has its own badge, colour and explanation:
  - **Not eligible**: solid red
  - **Eligible · risky**: dashed amber
  - **Eligible**: green
- [x] **Approval:** the button is enabled from the backend's `canApproveExpedited` flag. Structured errors (code and reasons) are displayed.
- [x] **Live updates:** after approval the UI updates the order and reloads the list. The list is also polled every 10 s, because eligibility changes over time. When polls overlap, the latest request wins.
- [x] **No recomputed logic:** there are no thresholds, scores or rules in `frontend/src`. The UI only displays what the API returns.

### Step 9: CORS and integration

- [x] **Development:** the Vite dev server proxies `/api` to `http://localhost:8080` (`frontend/vite.config.ts`). The browser only talks to its own origin, so it sends no cross-origin requests and no preflights.
- [x] **Separate origins** (production build or a different host): the backend `CorsConfig` sets an explicit allow-list from `app.cors.allowed-origins` (no `*`). It allows `GET`, `POST` and `OPTIONS` on `/api/**` and caches preflights for 1 h.
  - Override with `APP_CORS_ALLOWED_ORIGINS=https://ops.example.com`.
  - Point the UI at the API with `VITE_API_BASE_URL=https://api.example.com` (see `frontend/.env.example`).
- [x] Tests: the frontend origin passes preflight, and an unknown origin gets `403` with no CORS headers (`OrderControllerTest`). This was also checked in a browser, with the UI on `:4173` approving an order directly against `:8080`.

## API summary

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/orders` | All orders, each with current `eligibility` and `canApproveExpedited` |
| GET | `/api/orders/{id}` | One order with eligibility |
| GET | `/api/orders/{id}/eligibility` | Eligibility contract only |
| POST | `/api/orders/{id}/approve-expedited` | Approve (idempotent, re-evaluates) |
| POST | `/api/orders/{id}/inventory/refresh` | Demo stand-in for the inventory service. Body: `{"available": true}` (optional). |

## Assumptions to confirm

The brief says "same as before" for some rules and thresholds without listing them. These values are
assumed and configurable:

- **Tier thresholds:** STANDARD 500, GOLD 1500, PLATINUM 5000.
- **Previous hard rules:**
  - supported shipping countries
  - blocking risk flags
  - `REJECTED` orders cannot be expedited

  Each previous hard rule is a single class. If it does not apply, delete the class.
