import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert, MenuItem, Chip, Divider,
  Autocomplete, Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { downloadDocument, extractErrorMessage } from '../lib/api';
import { computeTaxBreakdown, type TaxLineInput } from '../lib/tax';
import TaxTotals from '../components/billing/TaxTotals';
import PurchasePaymentsDialog from '../components/billing/PurchasePaymentsDialog';
import Can from '../components/guards/Can';
import { ROLES } from '../lib/routeRoles';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import { PAYMENT_STATUSES } from '../types';
import type { Branch, PageResponse, PaymentStatus, Product, Purchase, Supplier } from '../types';

interface Line {
  productId: string;
  quantity: string;
  unitCost: string;
  extraCost: string;
}

export default function PurchasesPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [paymentsOpen, setPaymentsOpen] = useState(false);
  const [selectedPurchase, setSelectedPurchase] = useState<Purchase | null>(null);
  const [error, setError] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const [paymentStatus, setPaymentStatus] = useState('UNPAID');
  const [selectedBranchId, setSelectedBranchId] = useState<string>('');
  const [filterBranchId, setFilterBranchId] = useState<string>('');
  const [lines, setLines] = useState<Line[]>([{ productId: '', quantity: '1', unitCost: '0', extraCost: '0' }]);

  const { data: branches } = useQuery({
    queryKey: ['branches-purchases', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const effectiveDialogBranchId = selectedBranchId || branchId || branches?.[0]?.id || '';

  const { data, isLoading } = useQuery({
    queryKey: ['purchases', shopId, filterBranchId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Purchase> }>('/purchases', {
        params: { shopId, branchId: filterBranchId || undefined, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: suppliers } = useQuery({
    queryKey: ['suppliers-simple', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Supplier> }>('/suppliers', {
        params: { shopId, page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const { data: products } = useQuery({
    queryKey: ['products-simple', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Product> }>('/products', {
        params: { shopId, page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const hasValidLine = lines.some((l) => l.productId && Number(l.quantity) > 0);

  const breakdown = useMemo(() => {
    const productById = new Map((products ?? []).map((p) => [p.id, p]));
    const resolved: TaxLineInput[] = [];
    for (const line of lines) {
      const product = line.productId ? productById.get(line.productId) : undefined;
      if (!product || !(Number(line.quantity) > 0)) continue;
      resolved.push({
        quantity: Number(line.quantity),
        unitPrice: Number(line.unitCost || 0),
        vatApplicable: product.vatApplicable,
        vatRate: product.vatRate,
      });
    }
    return computeTaxBreakdown(resolved);
  }, [lines, products]);

  const create = useMutation({
    mutationFn: async () =>
      api.post('/purchases', {
        shopId,
        branchId: effectiveDialogBranchId,
        supplierId,
        paymentStatus,
        discountAmount: 0,
        items: lines
          .filter((l) => l.productId && Number(l.quantity) > 0)
          .map((l) => ({
            productId: l.productId,
            quantity: Number(l.quantity),
            unitCost: Number(l.unitCost),
            extraCost: Number(l.extraCost || 0),
          })),
      }),
    onSuccess: () => {
      setOpen(false);
      setLines([{ productId: '', quantity: '1', unitCost: '0', extraCost: '0' }]);
      setSupplierId('');
      queryClient.invalidateQueries({ queryKey: ['purchases'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const updatePaymentStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: PaymentStatus }) =>
      api.put(`/purchases/${id}/payment-status`, { paymentStatus: status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['purchases'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h5">Purchases</Typography>
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
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => {
          setSelectedBranchId(branchId || branches?.[0]?.id || '');
          setOpen(true);
        }}>
          New Purchase
        </Button>
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
                <TableCell>Number</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Supplier</TableCell>
                <TableCell>Branch</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.purchaseNumber}</TableCell>
                  <TableCell>{p.purchaseDate}</TableCell>
                  <TableCell>{p.supplierName}</TableCell>
                  <TableCell>{p.branchName}</TableCell>
                  <TableCell align="right">{p.totalAmount.toFixed(2)}</TableCell>
                  <TableCell>
                    <Can
                      roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.ACCOUNTANT]}
                      fallback={<Chip size="small" color={p.paymentStatus === 'PAID' ? 'success' : 'warning'} label={p.paymentStatus} />}
                    >
                      <TextField
                        select
                        size="small"
                        variant="standard"
                        value={p.paymentStatus}
                        disabled={updatePaymentStatus.isPending}
                        onChange={(e) => updatePaymentStatus.mutate({ id: p.id, status: e.target.value as PaymentStatus })}
                        sx={{ minWidth: 100 }}
                      >
                        {PAYMENT_STATUSES.map((s) => (
                          <MenuItem key={s} value={s}>
                            {s}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Can>
                  </TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => {
                        setSelectedPurchase(p);
                        setPaymentsOpen(true);
                      }}
                    >
                      Payments
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      sx={{ ml: 1 }}
                      onClick={() => downloadDocument(`/purchases/${p.id}/po/pdf`, 'purchase-order.pdf')}
                    >
                      PDF
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No purchases found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>New Purchase</DialogTitle>
        <DialogContent>
          {!effectiveDialogBranchId && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              No branch is selected. A purchase must be recorded against a branch.
              Create one under <strong>Administration → Branches</strong> first.
            </Alert>
          )}
          <Box sx={{ display: 'flex', gap: 1.5, mb: 2, flexWrap: 'wrap' }}>
            <TextField
              select
              label="Branch"
              required
              value={effectiveDialogBranchId}
              onChange={(e) => setSelectedBranchId(e.target.value)}
              sx={{ minWidth: 200, flex: 1 }}
            >
              {(branches ?? []).map((b) => (
                <MenuItem key={b.id} value={b.id}>
                  {b.name} ({b.code})
                </MenuItem>
              ))}
            </TextField>
            <Autocomplete
              options={suppliers ?? []}
              getOptionLabel={(option) => option.name}
              value={(suppliers ?? []).find((s) => s.id === supplierId) || null}
              onChange={(_, newValue) => setSupplierId(newValue?.id || '')}
              renderInput={(params) => <TextField {...params} label="Supplier" required />}
              sx={{ minWidth: 200, flex: 1 }}
            />
            <TextField
              select
              label="Payment Status"
              value={paymentStatus}
              onChange={(e) => setPaymentStatus(e.target.value)}
              sx={{ minWidth: 150 }}
            >
              {['UNPAID', 'PARTIAL', 'PAID'].map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          </Box>
          {lines.map((line, idx) => (
            <Box key={idx} sx={{ display: 'flex', gap: 1, mb: 1 }}>
              <Autocomplete
                options={products ?? []}
                getOptionLabel={(option) => `${option.name} (${option.sku})`}
                value={(products ?? []).find((p) => p.id === line.productId) || null}
                onChange={(_, newValue) => setLines(lines.map((l, i) => (i === idx ? { ...l, productId: newValue?.id || '' } : l)))}
                renderInput={(params) => <TextField {...params} label="Product" size="small" fullWidth />}
                fullWidth
                size="small"
                sx={{ flex: 1 }}
              />
              <TextField
                label="Qty"
                type="number"
                size="small"
                value={line.quantity}
                onChange={(e) => setLines(lines.map((l, i) => (i === idx ? { ...l, quantity: e.target.value } : l)))}
              />
              <TextField
                label="Unit Cost"
                type="number"
                size="small"
                value={line.unitCost}
                onChange={(e) => setLines(lines.map((l, i) => (i === idx ? { ...l, unitCost: e.target.value } : l)))}
              />
              <TextField
                label="Extra Cost"
                type="number"
                size="small"
                value={line.extraCost}
                onChange={(e) => setLines(lines.map((l, i) => (i === idx ? { ...l, extraCost: e.target.value } : l)))}
              />
              <IconButton
                onClick={() => setLines(lines.filter((_, i) => i !== idx))}
                disabled={lines.length === 1}
              >
                <DeleteIcon />
              </IconButton>
            </Box>
          ))}
          <Button onClick={() => setLines([...lines, { productId: '', quantity: '1', unitCost: '0', extraCost: '0' }])}>
            Add item
          </Button>
          <Divider sx={{ my: 1.5 }} />
          <TaxTotals
            subtotal={breakdown.subtotal}
            byRate={breakdown.byRate}
            tax={breakdown.vatAmount}
            total={breakdown.totalAmount}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Tooltip
            title={
              !effectiveDialogBranchId
                ? 'Select a branch before saving'
                : !supplierId
                  ? 'Choose a supplier first'
                  : !hasValidLine
                    ? 'Add at least one product with quantity'
                    : ''
            }
          >
            <span>
              <Button
                variant="contained"
                disabled={!supplierId || !effectiveDialogBranchId || !hasValidLine || create.isPending}
                onClick={() => create.mutate()}
              >
                Save
              </Button>
            </span>
          </Tooltip>
        </DialogActions>
      </Dialog>

      <PurchasePaymentsDialog
        open={paymentsOpen}
        onClose={() => setPaymentsOpen(false)}
        purchase={selectedPurchase}
      />
    </Box>
  );
}