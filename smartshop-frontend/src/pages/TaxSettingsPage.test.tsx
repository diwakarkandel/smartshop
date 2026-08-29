import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, CssBaseline } from '@mui/material';
import TaxSettingsPage from './TaxSettingsPage';
import theme from '../theme';
import { useShopStore } from '../stores/shopStore';
import type { Tax } from '../types';

const apiMock = vi.hoisted(() => ({
  listTaxes: vi.fn(),
  createTax: vi.fn(),
  updateTax: vi.fn(),
  addTaxRate: vi.fn(),
}));

vi.mock('../lib/api', () => ({
  ...apiMock,
  extractErrorMessage: (error: unknown) =>
    error instanceof Error ? error.message : 'Something went wrong',
}));

import { listTaxes, createTax, updateTax } from '../lib/api';

const taxRow = (id: string, name: string, isActive: boolean): Tax => ({
  id,
  shopId: 'shop-1',
  shopName: 'Smart Shop',
  name,
  type: 'PERCENTAGE',
  isActive,
  rates: [{ id: `r-${id}`, taxId: id, rate: 13, validFrom: '2026-01-01', validTo: null }],
});

const renderPage = () => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <TaxSettingsPage />
      </ThemeProvider>
    </QueryClientProvider>,
  );
};

beforeEach(() => {
  localStorage.clear();
  useShopStore.setState({ shopId: 'shop-1', branchId: null });
  vi.mocked(listTaxes).mockReset();
  vi.mocked(createTax).mockReset();
  vi.mocked(updateTax).mockReset();
  vi.mocked(listTaxes).mockResolvedValue([
    taxRow('t1', 'VAT 13%', true),
    taxRow('t2', 'Service Charge', false),
  ]);
  vi.stubGlobal('confirm', vi.fn(() => true));
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('TaxSettingsPage', () => {
  it('renders the list of tax rates with status chips', async () => {
    renderPage();
    expect(await screen.findByText('VAT 13%')).toBeInTheDocument();
    expect(screen.getByText('Service Charge')).toBeInTheDocument();
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByText('INACTIVE')).toBeInTheDocument();
    expect(screen.getAllByText('13% from 2026-01-01 (open-ended)')).toHaveLength(2);
  });

  it('opens the create dialog and submits a new tax', async () => {
    vi.mocked(createTax).mockResolvedValue(taxRow('t3', 'VAT 5%', true));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /new tax/i }));
    fireEvent.change(await screen.findByLabelText('Name'), { target: { value: 'VAT 5%' } });
    fireEvent.change(screen.getByLabelText('Rate %'), { target: { value: '5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => {
      expect(createTax).toHaveBeenCalledWith(
        expect.objectContaining({
          shopId: 'shop-1',
          name: 'VAT 5%',
          type: 'PERCENTAGE',
          isActive: true,
          rates: [expect.objectContaining({ rate: 5 })],
        }),
      );
    });
  });

  it('shows a validation error and does not submit without a name', async () => {
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: /new tax/i }));
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(createTax).not.toHaveBeenCalled();
  });

  it('deactivates a tax by flipping isActive to false', async () => {
    vi.mocked(updateTax).mockResolvedValue(taxRow('t1', 'VAT 13%', false));
    renderPage();

    const deactivateButtons = await screen.findAllByLabelText('Deactivate tax');
    fireEvent.click(deactivateButtons[0]);

    await waitFor(() => {
      expect(updateTax).toHaveBeenCalledWith(
        't1',
        expect.objectContaining({ name: 'VAT 13%', isActive: false }),
      );
    });
  });
});