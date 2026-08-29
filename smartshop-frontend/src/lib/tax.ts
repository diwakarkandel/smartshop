import type { SaleItem } from '../types';

export interface TaxLineInput {
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  vatApplicable?: boolean;
  vatRate: number;
}

export interface TaxRateRow {
  rate: number;
  taxable: number;
  vat: number;
}

export interface TaxBreakdown {
  subtotal: number;
  discountAmount: number;
  taxableAmount: number;
  vatAmount: number;
  totalAmount: number;
  byRate: TaxRateRow[];
}

export const round2 = (n: number): number => Math.round((n + Number.EPSILON) * 100) / 100;

export function computeTaxBreakdown(lines: TaxLineInput[], headerDiscount = 0): TaxBreakdown {
  let subtotal = 0;
  let vatAmount = 0;
  const rates = new Map<number, { taxable: number; vat: number }>();

  for (const line of lines) {
    const gross = round2(line.quantity * line.unitPrice);
    const lineDiscount = round2(line.discountAmount ?? 0);
    const taxable = round2(gross - lineDiscount);
    const rate = line.vatApplicable === false ? 0 : line.vatRate;
    const vat = round2((taxable * rate) / 100);

    subtotal = round2(subtotal + gross);
    vatAmount = round2(vatAmount + vat);

    const entry = rates.get(rate) ?? { taxable: 0, vat: 0 };
    entry.taxable = round2(entry.taxable + taxable);
    entry.vat = round2(entry.vat + vat);
    rates.set(rate, entry);
  }

  const byRate = [...rates.entries()]
    .map(([rate, value]) => ({ rate, taxable: value.taxable, vat: value.vat }))
    .sort((a, b) => a.rate - b.rate);

  const discountAmount = round2(headerDiscount);
  const taxableAmount = round2(subtotal - discountAmount);

  return {
    subtotal,
    discountAmount,
    taxableAmount,
    vatAmount,
    totalAmount: round2(taxableAmount + vatAmount),
    byRate,
  };
}

export function groupTaxByRate(items: SaleItem[]): TaxRateRow[] {
  const rates = new Map<number, { taxable: number; vat: number }>();
  for (const item of items) {
    const rate = item.vatRate ?? 0;
    const entry = rates.get(rate) ?? { taxable: 0, vat: 0 };
    entry.taxable = round2(entry.taxable + (item.taxableAmount ?? 0));
    entry.vat = round2(entry.vat + (item.vatAmount ?? 0));
    rates.set(rate, entry);
  }
  return [...rates.entries()]
    .map(([rate, value]) => ({ rate, taxable: value.taxable, vat: value.vat }))
    .sort((a, b) => a.rate - b.rate);
}