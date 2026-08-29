import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Dialog, DialogTitle, DialogContent, Chip, Pagination, CircularProgress,
} from '@mui/material';
import api from '../lib/api';
import { defaultShopId } from '../stores/shopStore';
import type { PageResponse, Sale } from '../types';

export default function SalesPage() {
  const shopId = defaultShopId();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Sale | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['sales', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Sale> }>('/sales', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Sales
      </Typography>
      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Invoice</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Branch</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell>Method</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((s) => (
                <TableRow key={s.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelected(s)}>
                  <TableCell>{s.invoiceNumber}</TableCell>
                  <TableCell>{s.billDate}</TableCell>
                  <TableCell>{s.branchName}</TableCell>
                  <TableCell>{s.customerName ?? '-'}</TableCell>
                  <TableCell align="right">{s.totalAmount.toFixed(2)}</TableCell>
                  <TableCell>{s.paymentMethod ?? '-'}</TableCell>
                  <TableCell>
                    <Chip size="small" color={s.paymentStatus === 'PAID' ? 'success' : 'warning'} label={s.paymentStatus} />
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>No sales found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={Boolean(selected)} onClose={() => setSelected(null)} fullWidth maxWidth="sm">
        <DialogTitle>{selected?.invoiceNumber}</DialogTitle>
        <DialogContent>
          {selected && (
            <>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Subtotal</Typography>
                <Typography variant="body2">{selected.subtotal.toFixed(2)}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Discount</Typography>
                <Typography variant="body2">{selected.discountAmount.toFixed(2)}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">VAT</Typography>
                <Typography variant="body2">{selected.vatAmount.toFixed(2)}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6">Total</Typography>
                <Typography variant="h6">{selected.totalAmount.toFixed(2)}</Typography>
              </Box>
              {selected.cashTendered != null && (
                <>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Tendered</Typography>
                    <Typography variant="body2">{selected.cashTendered.toFixed(2)}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body2">Change</Typography>
                    <Typography variant="body2" color="success.main">
                      {selected.changeAmount?.toFixed(2)}
                    </Typography>
                  </Box>
                </>
              )}
              <Typography variant="caption" color="text.secondary">
                Cashier: {selected.cashierName}
              </Typography>
            </>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
}