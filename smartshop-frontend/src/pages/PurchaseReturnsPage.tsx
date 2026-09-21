import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem,
  Alert, CircularProgress, Pagination, Divider,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type {
  ApiResponse, PageResponse, Purchase, PurchaseReturn,
  PurchaseReturnRequest, PurchaseItem,
} from '../types';

interface ReturnLine {
  purchaseItemId: string;
  productId: string;
  productName: string;
  maxQty: number;
  quantity: string;
}

export default function PurchaseReturnsPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [selectedPurchaseId, setSelectedPurchaseId] = useState('');
  const [reason, setReason] = useState('');
  const [lines, setLines] = useState<ReturnLine[]>([]);
  const [detail, setDetail] = useState<PurchaseReturn | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['purchase-returns', shopId, page],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PageResponse<PurchaseReturn>>>('/purchase-returns', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  // Load purchases for selection in "New Return" dialog
  const { data: purchases } = useQuery({
    queryKey: ['purchases-simple', shopId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PageResponse<Purchase>>>('/purchases', {
        params: { shopId, page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: open,
  });

  const { data: purchaseItems } = useQuery<Purchase>({
    queryKey: ['purchase-detail', selectedPurchaseId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Purchase>>(`/purchases/${selectedPurchaseId}`);
      return res.data.data;
    },
    enabled: Boolean(selectedPurchaseId),
  });

  useEffect(() => {
    if (purchaseItems?.items) {
      setLines(
        purchaseItems.items.map((item: PurchaseItem) => ({
          purchaseItemId: item.id,
          productId: item.productId,
          productName: item.productName,
          maxQty: item.quantity,
          quantity: '0',
        }))
      );
    }
  }, [purchaseItems]);

  const create = useMutation({
    mutationFn: async () => {
      const payload: PurchaseReturnRequest = {
        purchaseId: selectedPurchaseId,
        branchId: branchId!,
        reason: reason || undefined,
        items: lines
          .filter((l) => Number(l.quantity) > 0)
          .map((l) => ({
            purchaseItemId: l.purchaseItemId,
            productId: l.productId,
            quantity: Number(l.quantity),
          })),
      };
      await api.post('/purchase-returns', payload);
    },
    onSuccess: () => {
      setOpen(false);
      setSelectedPurchaseId('');
      setReason('');
      setLines([]);
      setError('');
      queryClient.invalidateQueries({ queryKey: ['purchase-returns'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const hasValidLines = lines.some((l) => Number(l.quantity) > 0);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Purchase Returns</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Return
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress /></Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Return #</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Purchase #</TableCell>
                <TableCell>Branch</TableCell>
                <TableCell>Reason</TableCell>
                <TableCell align="right">Refund Amount</TableCell>
                <TableCell>Created By</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((r) => (
                <TableRow
                  key={r.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => setDetail(r)}
                >
                  <TableCell>{r.returnNumber}</TableCell>
                  <TableCell>{r.returnDate}</TableCell>
                  <TableCell>{r.purchaseNumber}</TableCell>
                  <TableCell>{r.branchName}</TableCell>
                  <TableCell>{r.reason ?? '-'}</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>{r.refundAmount.toFixed(2)}</TableCell>
                  <TableCell>{r.createdByName}</TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                    No purchase returns found.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      {/* New Return Dialog */}
      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>New Purchase Return</DialogTitle>
        <DialogContent>
          <TextField
            select
            label="Purchase Order"
            fullWidth
            size="small"
            margin="dense"
            value={selectedPurchaseId}
            onChange={(e) => {
              setSelectedPurchaseId(e.target.value);
              setLines([]);
            }}
          >
            {(purchases ?? []).map((p) => (
              <MenuItem key={p.id} value={p.id}>
                {p.purchaseNumber} — {p.supplierName} ({p.purchaseDate})
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Reason (optional)"
            fullWidth
            size="small"
            margin="dense"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          />
          {selectedPurchaseId && lines.length > 0 && (
            <>
              <Divider sx={{ my: 1.5 }} />
              <Typography variant="subtitle2" sx={{ mb: 1 }}>Return Items (enter quantity to return)</Typography>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Product</TableCell>
                    <TableCell align="right">Original Qty</TableCell>
                    <TableCell align="right">Return Qty</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {lines.map((line, idx) => (
                    <TableRow key={line.purchaseItemId}>
                      <TableCell>{line.productName}</TableCell>
                      <TableCell align="right">{line.maxQty}</TableCell>
                      <TableCell align="right">
                        <TextField
                          type="number"
                          size="small"
                          inputProps={{ min: 0, max: line.maxQty, style: { textAlign: 'right', width: 80 } }}
                          value={line.quantity}
                          onChange={(e) =>
                            setLines(lines.map((l, i) => i === idx ? { ...l, quantity: e.target.value } : l))
                          }
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!selectedPurchaseId || !hasValidLines || create.isPending}
            onClick={() => create.mutate()}
          >
            Submit Return
          </Button>
        </DialogActions>
      </Dialog>

      {/* Detail dialog */}
      <Dialog open={Boolean(detail)} onClose={() => setDetail(null)} fullWidth maxWidth="sm">
        <DialogTitle>Return #{detail?.returnNumber}</DialogTitle>
        <DialogContent>
          {detail && (
            <>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2" color="text.secondary">Purchase</Typography>
                <Typography variant="body2">{detail.purchaseNumber}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2" color="text.secondary">Date</Typography>
                <Typography variant="body2">{detail.returnDate}</Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2" color="text.secondary">Branch</Typography>
                <Typography variant="body2">{detail.branchName}</Typography>
              </Box>
              {detail.reason && (
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2" color="text.secondary">Reason</Typography>
                  <Typography variant="body2">{detail.reason}</Typography>
                </Box>
              )}
              <Divider sx={{ my: 1.5 }} />
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Product</TableCell>
                    <TableCell align="right">Qty</TableCell>
                    <TableCell align="right">Unit Cost</TableCell>
                    <TableCell align="right">Total</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {detail.items.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        <Typography variant="body2">{item.productName}</Typography>
                        <Typography variant="caption" color="text.secondary">{item.sku}</Typography>
                      </TableCell>
                      <TableCell align="right">{item.quantity}</TableCell>
                      <TableCell align="right">{item.unitCost.toFixed(2)}</TableCell>
                      <TableCell align="right">{item.lineTotal.toFixed(2)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1.5 }}>
                <Typography variant="h6">Refund: {detail.refundAmount.toFixed(2)}</Typography>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetail(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
