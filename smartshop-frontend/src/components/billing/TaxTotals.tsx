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
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="body2">Tax</Typography>
        <Typography variant="body2">{tax.toFixed(2)}</Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h6">Grand Total</Typography>
        <Typography variant="h6">{total.toFixed(2)}</Typography>
      </Box>
    </Box>
  );
}