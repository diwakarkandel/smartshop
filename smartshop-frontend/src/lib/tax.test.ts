import { describe, it, expect } from 'vitest';
import { computeTaxBreakdown, groupTaxByRate, round2 } from './tax';

describe('round2', () => {
  it('rounds to two decimals HALF_UP', () => {
    expect(round2(0.005)).toBe(0.01);
    expect(round2(1.234)).toBe(1.23);
    expect(round2(1.235)).toBe(1.24);
  });
});

describe('computeTaxBreakdown', () => {
  it('computes single-rate breakdown matching backend math', () => {
    const result = computeTaxBreakdown([
      { quantity: 2, unitPrice: 150, vatRate: 13 },
    ]);
    expect(result.subtotal).toBe(300);
    expect(result.vatAmount).toBe(39);
    expect(result.totalAmount).toBe(339);
    expect(result.byRate).toEqual([{ rate: 13, taxable: 300, vat: 39 }]);
  });

  it('groups multiple lines by rate', () => {
    const result = computeTaxBreakdown([
      { quantity: 1, unitPrice: 100, vatRate: 13 },
      { quantity: 2, unitPrice: 50, vatRate: 13 },
      { quantity: 1, unitPrice: 20, vatRate: 0 },
    ]);
    expect(result.subtotal).toBe(220);
    expect(result.vatAmount).toBe(26);
    expect(result.totalAmount).toBe(246);
    expect(result.byRate).toEqual([
      { rate: 0, taxable: 20, vat: 0 },
      { rate: 13, taxable: 200, vat: 26 },
    ]);
    expect(result.subtotal + result.vatAmount).toBe(result.totalAmount);
  });

  it('treats vatApplicable=false as a zero rate', () => {
    const result = computeTaxBreakdown([
      { quantity: 1, unitPrice: 100, vatApplicable: false, vatRate: 13 },
    ]);
    expect(result.vatAmount).toBe(0);
    expect(result.byRate).toEqual([{ rate: 0, taxable: 100, vat: 0 }]);
  });

  it('applies per-line discounts to the tax base', () => {
    const result = computeTaxBreakdown([
      { quantity: 1, unitPrice: 100, discountAmount: 10, vatRate: 13 },
    ]);
    expect(result.vatAmount).toBe(11.7);
    expect(result.byRate[0].taxable).toBe(90);
    expect(result.subtotal).toBe(100);
    expect(result.totalAmount).toBe(111.7);
  });

  it('applies header discount to taxableAmount only (mirrors backend)', () => {
    const result = computeTaxBreakdown(
      [{ quantity: 10, unitPrice: 100, vatRate: 13 }],
      50,
    );
    expect(result.subtotal).toBe(1000);
    expect(result.discountAmount).toBe(50);
    expect(result.taxableAmount).toBe(950);
    expect(result.vatAmount).toBe(130);
    expect(result.totalAmount).toBe(1080);
  });

  it('handles empty cart', () => {
    const result = computeTaxBreakdown([]);
    expect(result).toEqual({
      subtotal: 0,
      discountAmount: 0,
      taxableAmount: 0,
      vatAmount: 0,
      totalAmount: 0,
      byRate: [],
    });
  });
});

describe('groupTaxByRate', () => {
  it('groups persisted sale items by rate for the receipt', () => {
    const rows = groupTaxByRate([
      { id: '1', productId: 'a', productName: 'A', sku: 'A1', quantity: 1, unitPrice: 100, vatRate: 13, vatAmount: 13, taxableAmount: 100, lineTotal: 113 },
      { id: '2', productId: 'b', productName: 'B', sku: 'B1', quantity: 1, unitPrice: 200, vatRate: 13, vatAmount: 26, taxableAmount: 200, lineTotal: 226 },
      { id: '3', productId: 'c', productName: 'C', sku: 'C1', quantity: 1, unitPrice: 20, vatRate: 0, vatAmount: 0, taxableAmount: 20, lineTotal: 20 },
    ]);
    expect(rows).toEqual([
      { rate: 0, taxable: 20, vat: 0 },
      { rate: 13, taxable: 300, vat: 39 },
    ]);
  });
});