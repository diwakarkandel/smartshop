import { useMutation } from '@tanstack/react-query';
import {
  Box, Card, Typography, TextField, Button, Alert, MenuItem,
} from '@mui/material';
import { useState } from 'react';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';

const JOINABLE_ROLES = ['CASHIER', 'MANAGER', 'INVENTORY_STAFF'];

export default function JoinShopPage() {
  const user = useAuthStore((s) => s.user);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [form, setForm] = useState({ inviteCode: '', requestedRole: 'CASHIER' });

  const isAdmin = user?.roles.includes('SUPER_ADMIN') || user?.roles.includes('SHOP_ADMIN');

  const join = useMutation({
    mutationFn: async () =>
      api.post('/staff-invitations/join', {
        inviteCode: form.inviteCode.trim(),
        requestedRole: form.requestedRole,
      }),
    onSuccess: () => {
      setError('');
      setSubmitted(true);
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  if (isAdmin) {
    return (
      <Alert severity="info">
        You already manage a shop, so you don&apos;t need to join one.
      </Alert>
    );
  }

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
      <Card sx={{ p: 4, maxWidth: 420, width: '100%' }}>
        {submitted ? (
          <>
            <Alert severity="success" sx={{ mb: 2 }}>
              Request sent! Your shop owner will review and approve your request. You&apos;ll
              receive an email notification.
            </Alert>
            <Button fullWidth variant="outlined" onClick={() => setSubmitted(false)}>
              Send Another Request
            </Button>
          </>
        ) : (
          <>
            <Typography variant="h5" gutterBottom align="center">
              Join a Shop
            </Typography>
            <Typography variant="body2" color="text.secondary" align="center" sx={{ mb: 3 }}>
              Enter the invite code provided by your shop owner to request access.
            </Typography>

            {error && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
                {error}
              </Alert>
            )}

            <TextField
              label="Invite Code"
              required
              fullWidth
              margin="dense"
              placeholder="SHOP-XXXXX"
              value={form.inviteCode}
              onChange={(e) => setForm({ ...form, inviteCode: e.target.value.toUpperCase() })}
              sx={{ '& input': { fontFamily: 'monospace', letterSpacing: 2 } }}
            />
            <TextField
              select
              label="Requested Role"
              fullWidth
              margin="dense"
              value={form.requestedRole}
              onChange={(e) => setForm({ ...form, requestedRole: e.target.value })}
            >
              {JOINABLE_ROLES.map((r) => (
                <MenuItem key={r} value={r}>
                  {r === 'INVENTORY_STAFF' ? 'Inventory Staff' : r}
                </MenuItem>
              ))}
            </TextField>
            <Button
              fullWidth
              variant="contained"
              sx={{ mt: 2 }}
              disabled={join.isPending || !form.inviteCode.trim()}
              onClick={() => join.mutate()}
            >
              {join.isPending ? 'Sending...' : 'Send Join Request'}
            </Button>
          </>
        )}
      </Card>
    </Box>
  );
}
