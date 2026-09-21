import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Dialog, DialogTitle, DialogContent, DialogActions, Button, Chip, Pagination, CircularProgress,
  Divider, TextField, MenuItem, Alert, InputAdornment,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import api, { downloadDocument, extractErrorMessage } from '../lib/api';
import { groupTaxByRate } from '../lib/tax';
import TaxTotals from '../components/billing/TaxTotals';
import SalesPaymentsDialog from '../components/billing/SalesPaymentsDialog';
import Can from '../components/guards/Can';
import { ROLES } from '../lib/routeRoles';
import { useDefaultShopId } from '../stores/shopStore';
import { PAYMENT_STATUSES } from '../types';
import type { Branch, PageResponse, PaymentStatus, Sale } from '../types';

export default function SalesPage() {
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Sale | null>(null);
  const [paymentsOpen, setPaymentsOpen] = useState(false);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [filterBranchId, setFilterBranchId] = useState('');

  const { data: branches } = useQuery({
    queryKey: ['branches-sales', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data, isLoading } = useQuery({
    queryKey: ['sales', shopId, filterBranchId, search, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Sale> }>('/sales', {
        params: {
          shopId,
          branchId: filterBranchId || undefined,
          search: search.trim() || undefined,
          page,
          size: 10,
        },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const setPaymentStatus = useMutation({
    mutationFn: ({ id, paymentStatus }: { id: string; paymentStatus: PaymentStatus }) =>
      api.put(`/sales/${id}/payment-status`, { paymentStatus }),
    onSuccess: (_res, vars) => {
      setSelected((s) => (s ? { ...s, paymentStatus: vars.paymentStatus } : s));
      queryClient.invalidateQueries({ queryKey: ['sales'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
        <Typography variant="h5">Sales</Typography>
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap' }}>
          <TextField
            size="small"
            placeholder="Search by invoice, customer, phone..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
            }}
            sx={{ minWidth: 280 }}
          />
          {(branches ?? []).length > 0 && (
            <TextField
              select
              size="small"
              label="Filter by Branch"
              value={filterBranchId}
              onChange={(e) => {
                setFilterBranchId(e.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 180 }}
            >
              <MenuItem value="">All Branches</MenuItem>
              {branches?.map((b) => (
                <MenuItem key={b.id} value={b.id}>
                  {b.name} ({b.code})
                </MenuItem>
              ))}
            </TextField>
          )}
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
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
                  <TableCell>
                    {s.customerName ? (
                      <Box>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>{s.customerName}</Typography>
                        {s.customerPhone && (
                          <Typography variant="caption" color="text.secondary">📞 {s.customerPhone}</Typography>
                        )}
                      </Box>
                    ) : (
                      <Typography variant="body2" color="text.secondary">Walk-in</Typography>
                    )}
                  </TableCell>
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
                <Typography variant="body2">Customer</Typography>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {selected.customerName
                    ? `${selected.customerName}${selected.customerPhone ? ` (📞 ${selected.customerPhone})` : ''}`
                    : 'Walk-in Customer'}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Payment</Typography>
                <Typography variant="body2">
                  {selected.paymentMethod ?? '-'}
                  {selected.paymentStatus === 'PAID' ? ' (PAID)' : ` (${selected.paymentStatus})`}
                </Typography>
              </Box>
              <Can roles={[ROLES.SHOP_ADMIN, ROLES.ACCOUNTANT]}>
                <TextField
                  select
                  size="small"
                  label="Set payment status"
                  fullWidth
                  margin="dense"
                  value={selected.paymentStatus}
                  disabled={setPaymentStatus.isPending}
                  onChange={(e) =>
                    setPaymentStatus.mutate({ id: selected.id, paymentStatus: e.target.value as PaymentStatus })
                  }
                >
                  {PAYMENT_STATUSES.map((s) => (
                    <MenuItem key={s} value={s}>
                      {s}
                    </MenuItem>
                  ))}
                </TextField>
              </Can>
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
        <DialogActions>
          <Button 
            variant="outlined" 
            onClick={() => setPaymentsOpen(true)}
            sx={{ mr: 'auto' }}
          >
            Payments
          </Button>
          <Button
            variant="outlined"
            disabled={!selected}
            onClick={() => selected && downloadDocument(`/sales/${selected.id}/invoice/pdf`, 'sale-invoice.pdf')}
          >
            PDF Invoice
          </Button>
          <Button onClick={() => setSelected(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      <SalesPaymentsDialog
        open={paymentsOpen}
        onClose={() => setPaymentsOpen(false)}
        sale={selected}
      />
    </Box>
  );
}