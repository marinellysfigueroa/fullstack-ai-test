// Mirrors the backend DTOs. The UI only displays these values; it never derives eligibility itself.

export type CustomerTier = 'STANDARD' | 'GOLD' | 'PLATINUM';
export type OrderStatus = 'NEW' | 'REVIEW' | 'APPROVED' | 'REJECTED';
export type Decision = 'ELIGIBLE' | 'ELIGIBLE_WITH_RISK' | 'NOT_ELIGIBLE';
export type Severity = 'BLOCKING' | 'PENALTY' | 'WARNING';

export interface Finding {
  ruleId: string;
  severity: Severity;
  message: string;
  penalty: number;
}

export interface Eligibility {
  eligible: boolean;
  score: number;
  reasons: string[];
  warnings: string[];
  decision: Decision;
  blockingReasons: string[];
  findings: Finding[];
  evaluatedAt: string;
}

export interface OrderItem {
  sku: string | null;
  name: string | null;
  quantity: number | null;
  unitPrice: number | null;
}

export interface ExpeditedApproval {
  approvedAt: string;
  scoreAtApproval: number;
  warningsAtApproval: string[];
}

export interface Order {
  orderId: string;
  customer: { name: string | null; tier: CustomerTier | null } | null;
  items: OrderItem[] | null;
  totalAmount: number | null;
  shipping: { country: string | null; addressValidated: boolean | null } | null;
  riskFlags: string[] | null;
  inventoryStatus: { available: boolean; lastCheckedAt: string | null } | null;
  status: OrderStatus;
  createdAt: string | null;
  expeditedApproval: ExpeditedApproval | null;
  version: number;
  eligibility: Eligibility;
  canApproveExpedited: boolean;
}

export interface ApprovalResponse {
  orderId: string;
  status: OrderStatus;
  alreadyApproved: boolean;
  approval: ExpeditedApproval | null;
  eligibility: Eligibility | null;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  orderId?: string;
  reasons?: string[];
  warnings?: string[];
  eligibility?: Eligibility;
  timestamp?: string;
}
