import { Box, Typography } from '@mui/material';
import type { TaxRateRow } from '../../lib/tax';

interface TaxTotalsProps {
  subtotal: number;
  byRate: TaxRateRow[];
  tax: number;
  total: number;
  discount?: number;
}

export default function TaxTotals({ subtotal, byRate, tax, total, discount }: TaxTotalsProps) {
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="body2">Subtotal</Typography>
        <Typography variant="body2">{subtotal.toFixed(2)}</Typography>
      </Box>
      {discount != null && discount > 0 && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="body2">Discount</Typography>
          <Typography variant="body2">-{discount.toFixed(2)}</Typography>
        </Box>
      )}
      {byRate.map((row) => (
        <Box key={row.rate} sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="body2">
            {row.rate === 0 ? 'Taxable (0%)' : `Tax ${row.rate}%`}
          </Typography>
          <Typography variant="body2">{row.vat.toFixed(2)}</Typography>
        </Box>
      ))}
      {/* Show an aggregate tax line when several rates are combined, or when there
          are no per-rate rows to fall back on. A single rate row is skipped ONLY
          when its own vat already equals the aggregate — otherwise (inconsistent
          data) we still surface the real total rather than hiding it. */}
      {(byRate.length > 1 ||
        byRate.length === 0 ||
        Math.abs((byRate[0]?.vat ?? 0) - tax) > 0.005) && (tax > 0 || byRate.length > 1) && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="body2" fontWeight={byRate.length > 1 ? 600 : 400}>
            {byRate.length > 1 ? 'Total Tax' : 'Tax'}
          </Typography>
          <Typography variant="body2" fontWeight={byRate.length > 1 ? 600 : 400}>
            {tax.toFixed(2)}
          </Typography>
        </Box>
      )}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h6">Grand Total</Typography>
        <Typography variant="h6">{total.toFixed(2)}</Typography>
      </Box>
    </Box>
  );
}