import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { order } from '../test/fixtures';
import { OrderDetail } from './OrderDetail';

const jsonResponse = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

describe('OrderDetail approval', () => {
  afterEach(() => vi.restoreAllMocks());

  it('approves and asks the parent to re-sync', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(200, { orderId: 'ORD-1', status: 'APPROVED', alreadyApproved: false, approval: null, eligibility: { score: 100 } }),
    );
    const onChanged = vi.fn();
    render(<OrderDetail order={order()} onChanged={onChanged} />);

    await userEvent.click(screen.getByRole('button', { name: 'Approve expedited' }));

    expect(await screen.findByRole('status')).toHaveTextContent('ORD-1 approved for expedited processing');
    expect(fetchMock).toHaveBeenCalledWith('/api/orders/ORD-1/approve-expedited', expect.objectContaining({ method: 'POST' }));
    await waitFor(() => expect(onChanged).toHaveBeenCalled());
  });

  it('shows the structured error when the order became ineligible', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(422, {
        status: 422,
        code: 'NOT_ELIGIBLE',
        message: 'Order ORD-1 is not eligible for expedited processing',
        reasons: ['Inventory check is stale: last checked 12 min ago (maximum 10 min)'],
      }),
    );
    const onChanged = vi.fn();
    render(<OrderDetail order={order()} onChanged={onChanged} />);

    await userEvent.click(screen.getByRole('button', { name: 'Approve expedited' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('NOT_ELIGIBLE');
    expect(alert).toHaveTextContent('Inventory check is stale');
    await waitFor(() => expect(onChanged).toHaveBeenCalled());
  });

  it('trusts the backend flag to disable approval', () => {
    render(<OrderDetail order={order({ canApproveExpedited: false })} onChanged={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Approve expedited' })).toBeDisabled();
  });
});
