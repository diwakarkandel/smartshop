import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Button,
  Table, TableHead, TableRow, TableCell, TableBody,
  Typography, Box, CircularProgress, Alert, TextField, MenuItem,
  Chip
} from '@mui/material';
import api, { extractErrorMessage } from '../../lib/api';
import type { Payment, Sale } from '../../types';
import { PAYMENT_METHODS } from '../../types';

interface Props {
  open: boolean;
  onClose: () => void;
  sale: Sale | null;
}

export default function SalesPaymentsDialog({ open, onClose, sale }: Props) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [form, setForm] = useState({
    amount: '',
    paymentMethod: 'CASH',
    referenceNumber: ''
  });

  const { data: payments, isLoading } = useQuery({
    queryKey: ['sales-payments', sale?.id],
    queryFn: async () => {
      const res = await api.get<{ data: Payment[] }>(`/payments`, {
        params: { saleId: sale?.id }
      });
      return res.data.data;
    },
    enabled: Boolean(sale?.id && open),
  });

  const totalPaid = (payments ?? []).reduce((sum, p) => sum + p.amount, 0);
  const totalAmount = sale?.totalAmount ?? 0;
  const balanceDue = Math.max(0, totalAmount - totalPaid);

  const addPayment = useMutation({
    mutationFn: async () => {
      const payload = {
        saleId: sale?.id,
        amount: Number(form.amount),
        paymentMethod: form.paymentMethod,
        referenceNumber: form.referenceNumber || undefined,
      };
      await api.post(`/payments`, payload);
    },
    onSuccess: () => {
      setIsAdding(false);
      setError('');
      setForm({ ...form, amount: '', referenceNumber: '' });
      queryClient.invalidateQueries({ queryKey: ['sales-payments', sale?.id] });
      queryClient.invalidateQueries({ queryKey: ['sales'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const handleOpenAdd = () => {
    setForm(f => ({ ...f, amount: balanceDue > 0 ? balanceDue.toString() : '' }));
    setIsAdding(true);
  };

  if (!sale) return null;

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>
        Payments for Sale #{sale.invoiceNumber}
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
                label="Amount" 
                type="number" 
                size="small" 
                fullWidth 
                value={form.amount}
                onChange={e => setForm({ ...form, amount: e.target.value })}
              />
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
            </Box>
            <TextField 
              label="Reference No" 
              size="small" 
              fullWidth 
              value={form.referenceNumber}
              onChange={e => setForm({ ...form, referenceNumber: e.target.value })}
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
                    <TableCell>Status</TableCell>
                    <TableCell>Ref</TableCell>
                    <TableCell align="right">Amount</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(payments ?? []).map(p => (
                    <TableRow key={p.id}>
                      <TableCell>{new Date(p.paidAt).toLocaleDateString()}</TableCell>
                      <TableCell>
                        <Chip size="small" label={p.paymentMethod} />
                      </TableCell>
                      <TableCell>
                        <Chip size="small" variant="outlined" color={p.paymentStatus === 'COMPLETED' ? 'success' : 'default'} label={p.paymentStatus} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">{p.referenceNumber || '-'}</Typography>
                      </TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>{p.amount.toFixed(2)}</TableCell>
                    </TableRow>
                  ))}
                  {(payments ?? []).length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
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
