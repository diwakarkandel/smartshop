import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  Table, TableHead, TableRow, TableCell, TableBody,
  Typography, Box, CircularProgress, Alert, TextField, MenuItem,
  Chip
} from '@mui/material';
import api, { extractErrorMessage } from '../../lib/api';
import type { PurchasePayment, Purchase } from '../../types';
import { PAYMENT_METHODS } from '../../types';

interface Props {
  open: boolean;
  onClose: () => void;
  purchase: Purchase | null;
}

export default function PurchasePaymentsDialog({ open, onClose, purchase }: Props) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [form, setForm] = useState({
    paymentDate: new Date().toISOString().split('T')[0],
    amount: '',
    paymentMethod: 'CASH',
    referenceNumber: '',
    notes: ''
  });

  const { data: payments, isLoading } = useQuery({
    queryKey: ['purchase-payments', purchase?.id],
    queryFn: async () => {
      const res = await api.get<{ data: PurchasePayment[] }>(`/purchases/${purchase?.id}/payments`);
      return res.data.data;
    },
    enabled: Boolean(purchase?.id && open),
  });

  const totalPaid = (payments ?? []).reduce((sum, p) => sum + p.amount, 0);
  const totalAmount = purchase?.totalAmount ?? 0;
  const balanceDue = Math.max(0, totalAmount - totalPaid);

  const addPayment = useMutation({
    mutationFn: async () => {
      const payload = {
        paymentDate: form.paymentDate,
        amount: Number(form.amount),
        paymentMethod: form.paymentMethod,
        referenceNumber: form.referenceNumber || undefined,
        notes: form.notes || undefined,
      };
      await api.post(`/purchases/${purchase?.id}/payments`, payload);
    },
    onSuccess: () => {
      setIsAdding(false);
      setError('');
      setForm({ ...form, amount: '', referenceNumber: '', notes: '' });
      queryClient.invalidateQueries({ queryKey: ['purchase-payments', purchase?.id] });
      queryClient.invalidateQueries({ queryKey: ['purchases'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const handleOpenAdd = () => {
    setForm(f => ({ ...f, amount: balanceDue > 0 ? balanceDue.toString() : '' }));
    setIsAdding(true);
  };

  if (!purchase) return null;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>
        Payments for Purchase #{purchase.purchaseNumber}
      </DialogTitle>
      <DialogContent>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2, p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
          <Box>
            <Typography variant="body2" color="text.secondary">Total Amount</Typography>
            <Typography variant="h6">{totalAmount.toFixed(2)}</Typography>
          </Box>
          <Box>
            <Typography variant="body2" color="text.secondary">Total Paid</Typography>
            <Typography variant="h6" color="success.main">{totalPaid.toFixed(2)}</Typography>
          </Box>
          <Box>
            <Typography variant="body2" color="text.secondary">Balance Due</Typography>
            <Typography variant="h6" color={balanceDue > 0 ? "error.main" : "text.primary"}>
              {balanceDue.toFixed(2)}
            </Typography>
          </Box>
        </Box>

        {isAdding ? (
          <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="subtitle2">Record New Payment</Typography>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField 
                label="Date" 
                type="date" 
                size="small" 
                fullWidth 
                InputLabelProps={{ shrink: true }}
                value={form.paymentDate}
                onChange={e => setForm({ ...form, paymentDate: e.target.value })}
              />
              <TextField 
                label="Amount" 
                type="number" 
                size="small" 
                fullWidth 
                value={form.amount}
                onChange={e => setForm({ ...form, amount: e.target.value })}
              />
            </Box>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField 
                select 
                label="Method" 
                size="small" 
                fullWidth 
                value={form.paymentMethod}
                onChange={e => setForm({ ...form, paymentMethod: e.target.value })}
              >
                {PAYMENT_METHODS.map(m => <MenuItem key={m} value={m}>{m}</MenuItem>)}
              </TextField>
              <TextField 
                label="Reference No" 
                size="small" 
                fullWidth 
                value={form.referenceNumber}
                onChange={e => setForm({ ...form, referenceNumber: e.target.value })}
              />
            </Box>
            <TextField 
              label="Notes" 
              size="small" 
              multiline 
              rows={2} 
              fullWidth 
              value={form.notes}
              onChange={e => setForm({ ...form, notes: e.target.value })}
            />
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
              <Button size="small" onClick={() => setIsAdding(false)}>Cancel</Button>
              <Button 
                variant="contained" 
                size="small" 
                disabled={!form.amount || Number(form.amount) <= 0 || addPayment.isPending}
                onClick={() => addPayment.mutate()}
              >
                Save Payment
              </Button>
            </Box>
          </Box>
        ) : (
          <>
            {isLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress size={24} />
              </Box>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Method</TableCell>
                    <TableCell>Ref/Notes</TableCell>
                    <TableCell align="right">Amount</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(payments ?? []).map(p => (
                    <TableRow key={p.id}>
                      <TableCell>{p.paymentDate}</TableCell>
                      <TableCell>
                        <Chip size="small" label={p.paymentMethod} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{p.referenceNumber || '-'}</Typography>
                        {p.notes && <Typography variant="caption" color="text.secondary">{p.notes}</Typography>}
                      </TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>{p.amount.toFixed(2)}</TableCell>
                    </TableRow>
                  ))}
                  {(payments ?? []).length === 0 && (
                    <TableRow>
                      <TableCell colSpan={4} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                        No payments recorded yet.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}
          </>
        )}
      </DialogContent>
      <DialogActions>
        {!isAdding && balanceDue > 0 && (
          <Button variant="contained" onClick={handleOpenAdd} sx={{ mr: 'auto' }}>
            Add Payment
          </Button>
        )}
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
