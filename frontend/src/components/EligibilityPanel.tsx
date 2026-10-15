import type { Eligibility, Finding, Severity } from '../api/types';
import { DecisionBadge } from './Badges';

const DECISION_SUMMARY = {
  ELIGIBLE: 'Can be expedited. No risk signals were found.',
  ELIGIBLE_WITH_RISK: 'Can be expedited, but review the deductions and warnings below before approving.',
  NOT_ELIGIBLE: 'Cannot be expedited until every blocking issue is resolved.',
} as const;

const SECTIONS: { severity: Severity; title: string; empty?: string }[] = [
  { severity: 'BLOCKING', title: 'Blocking issues', empty: 'No blocking issues' },
  { severity: 'PENALTY', title: 'Score deductions' },
  { severity: 'WARNING', title: 'Warnings' },
];

/** Renders the backend's evaluation as-is: findings are grouped by the severity the backend assigned. */
export function EligibilityPanel({ eligibility }: { eligibility: Eligibility }) {
  const bySeverity = (severity: Severity): Finding[] =>
    eligibility.findings.filter((f) => f.severity === severity);

  return (
    <section className={`eligibility eligibility-${eligibility.decision.toLowerCase()}`} aria-label="Eligibility">
      <header className="eligibility-header">
        <div>
          <DecisionBadge decision={eligibility.decision} />
          <p className="eligibility-summary">{DECISION_SUMMARY[eligibility.decision]}</p>
        </div>
        <div className="score" aria-label={`Score ${eligibility.score} of 100`}>
          <span className="score-value">{eligibility.score}</span>
          <span className="score-max">/100</span>
          <div className="score-bar">
            <div className="score-fill" style={{ width: `${eligibility.score}%` }} />
          </div>
        </div>
      </header>

      {SECTIONS.map(({ severity, title, empty }) => {
        const findings = bySeverity(severity);
        if (findings.length === 0 && !empty) return null;
        return (
          <div
            key={severity}
            className={`findings findings-${severity.toLowerCase()}${findings.length === 0 ? ' findings-empty' : ''}`}
          >
            <h4>
              {title} {findings.length > 0 && <span className="count">{findings.length}</span>}
            </h4>
            {findings.length === 0 ? (
              <p className="muted">{empty}</p>
            ) : (
              <ul>
                {findings.map((f, i) => (
                  <li key={`${f.ruleId}-${i}`}>
                    {f.message}
                    <code className="rule-id">{f.ruleId}</code>
                  </li>
                ))}
              </ul>
            )}
          </div>
        );
      })}

      <p className="muted small">Evaluated by the server at {new Date(eligibility.evaluatedAt).toLocaleTimeString()}</p>
    </section>
  );
}
