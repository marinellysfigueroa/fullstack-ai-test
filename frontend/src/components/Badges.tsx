import type { Decision, OrderStatus } from '../api/types';

const DECISION_LABELS: Record<Decision, string> = {
  ELIGIBLE: 'Eligible',
  ELIGIBLE_WITH_RISK: 'Eligible · risky',
  NOT_ELIGIBLE: 'Not eligible',
};

export function DecisionBadge({ decision }: { decision: Decision }) {
  return <span className={`badge decision-${decision.toLowerCase()}`}>{DECISION_LABELS[decision]}</span>;
}

export function StatusBadge({ status }: { status: OrderStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{status}</span>;
}
