import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { eligibility } from '../test/fixtures';
import { EligibilityPanel } from './EligibilityPanel';

describe('EligibilityPanel', () => {
  it('separates blocking issues, deductions and warnings exactly as the backend classified them', () => {
    render(
      <EligibilityPanel
        eligibility={eligibility({
          eligible: false,
          score: 45,
          decision: 'NOT_ELIGIBLE',
          findings: [
            { ruleId: 'INVENTORY_AVAILABLE', severity: 'BLOCKING', message: 'Inventory is not available', penalty: 0 },
            { ruleId: 'HIGH_QUANTITY', severity: 'PENALTY', message: 'Total quantity 5 exceeds 3 (-30)', penalty: 30 },
            { ruleId: 'RISK_ASSESSMENT_MISSING', severity: 'WARNING', message: 'Risk assessment is missing', penalty: 0 },
          ],
        })}
      />,
    );

    expect(screen.getByText('Not eligible')).toBeInTheDocument();
    expect(screen.getByText('45')).toBeInTheDocument();
    const blocking = screen.getByText('Blocking issues').closest('div')!;
    expect(within(blocking).getByText('Inventory is not available')).toBeInTheDocument();
    const deductions = screen.getByText('Score deductions').closest('div')!;
    expect(within(deductions).getByText(/Total quantity 5/)).toBeInTheDocument();
    const warnings = screen.getByText('Warnings').closest('div')!;
    expect(within(warnings).getByText('Risk assessment is missing')).toBeInTheDocument();
  });

  it('shows "eligible but risky" distinctly from "not eligible"', () => {
    render(<EligibilityPanel eligibility={eligibility({ score: 70, decision: 'ELIGIBLE_WITH_RISK' })} />);

    expect(screen.getByText('Eligible · risky')).toBeInTheDocument();
    expect(screen.getByText('No blocking issues')).toBeInTheDocument();
    expect(screen.queryByText('Not eligible')).not.toBeInTheDocument();
  });
});
