import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Dialog, DialogTitle, DialogContent, Chip, Pagination, CircularProgress, Divider,
} from '@mui/material';
import api from '../lib/api';
import { groupTaxByRate } from '../lib/tax';
import TaxTotals from '../components/billing/TaxTotals';
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
                <Typography variant="body2">Date</Typography>
                <Typography variant="body2">{selected.billDate}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Branch</Typography>
                <Typography variant="body2">{selected.branchName}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Payment</Typography>
                <Typography variant="body2">
                  {selected.paymentMethod ?? '-'}
                  {selected.paymentStatus === 'PAID' ? ' (PAID)' : ` (${selected.paymentStatus})`}
                </Typography>
              </Box>
              <Divider sx={{ my: 1.5 }} />
              {(selected.items ?? []).length > 0 && (
                <>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Item</TableCell>
                        <TableCell align="right">Qty</TableCell>
                        <TableCell align="right">Price</TableCell>
                        <TableCell align="right">Tax</TableCell>
                        <TableCell align="right">Line</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {(selected.items ?? []).map((item) => (
                        <TableRow key={item.id}>
                          <TableCell>
                            <Typography variant="body2">{item.productName}</Typography>
                            <Typography variant="caption" color="text.secondary">
                              {item.sku}
                            </Typography>
                          </TableCell>
                          <TableCell align="right">{item.quantity}</TableCell>
                          <TableCell align="right">{item.unitPrice.toFixed(2)}</TableCell>
                          <TableCell align="right">
                            {item.vatRate != null ? `${item.vatRate}% / ${item.vatAmount?.toFixed(2)}` : '-'}
                          </TableCell>
                          <TableCell align="right">{item.lineTotal.toFixed(2)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <Divider sx={{ my: 1.5 }} />
                </>
              )}
              <TaxTotals
                subtotal={selected.subtotal}
                discount={selected.discountAmount > 0 ? selected.discountAmount : undefined}
                byRate={groupTaxByRate(selected.items ?? [])}
                tax={selected.vatAmount}
                total={selected.totalAmount}
              />
              {selected.cashTendered != null && (
                <>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Tendered</Typography>
                    <Typography variant="body2">{selected.cashTendered.toFixed(2)}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Change</Typography>
                    <Typography variant="body2" color="success.main">
                      {selected.changeAmount?.toFixed(2)}
                    </Typography>
                  </Box>
                </>
              )}
              <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="caption" color="text.secondary">
                  Cashier: {selected.cashierName}
                </Typography>
              </Box>
            </>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
}