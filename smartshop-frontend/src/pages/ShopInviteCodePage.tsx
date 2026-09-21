import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Button, Alert, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, Divider,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useState } from 'react';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { useDefaultShopId } from '../stores/shopStore';
import type { ApiResponse, InviteCode } from '../types';

export default function ShopInviteCodePage() {
  const hasRole = useAuthStore((s) => s.hasRole);
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [copied, setCopied] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data: inviteCode, isLoading } = useQuery({
    queryKey: ['invite-code', shopId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<InviteCode>>(`/shops/${shopId}/invite-code`);
      return res.data.data;
    },
    enabled: hasRole('SHOP_ADMIN') && Boolean(shopId),
  });

  const regenerate = useMutation({
    mutationFn: async () => api.post(`/shops/${shopId}/invite-code/regenerate`),
    onSuccess: () => {
      setConfirmOpen(false);
      setCopied(false);
      setSuccess('New invite code generated. The old code is no longer valid.');
      queryClient.invalidateQueries({ queryKey: ['invite-code', shopId] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  if (!hasRole('SHOP_ADMIN')) {
    return <Alert severity="error">Access Denied. This page is for Shop Admins only.</Alert>;
  }

  const handleCopy = async () => {
    if (!inviteCode) return;
    try {
      await navigator.clipboard.writeText(inviteCode.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setError('Could not copy to clipboard. Please copy the code manually.');
    }
  };

  return (
    <Box sx={{ maxWidth: 640, mx: 'auto' }}>
      <Typography variant="h5" gutterBottom>
        Your Shop Invite Code
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
          {success}
        </Alert>
      )}

      {!shopId ? (
        <Alert severity="warning">No shop found for your account.</Alert>
      ) : isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Card sx={{ p: 3 }}>
          <Box
            sx={{
              bgcolor: 'grey.100',
              border: '2px dashed',
              borderColor: 'primary.main',
              borderRadius: 2,
              p: 4,
              textAlign: 'center',
              mb: 2,
            }}
          >
            <Typography
              variant="h3"
              component="div"
              sx={{ fontFamily: 'monospace', fontWeight: 700, letterSpacing: 4 }}
            >
              {inviteCode?.code ?? '-'}
            </Typography>
            {inviteCode && !inviteCode.active && (
              <Typography variant="caption" color="error">
                This code is inactive
              </Typography>
            )}
          </Box>

          <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mb: 3 }}>
            <Button
              variant="contained"
              startIcon={<ContentCopyIcon />}
              onClick={handleCopy}
            >
              {copied ? 'Copied!' : 'Copy to Clipboard'}
            </Button>
            <Button
              variant="outlined"
              color="warning"
              startIcon={<RefreshIcon />}
              onClick={() => setConfirmOpen(true)}
            >
              Regenerate Code
            </Button>
          </Box>

          <Divider sx={{ mb: 2 }} />

          <Typography variant="subtitle2" gutterBottom>
            How staff can join your shop:
          </Typography>
          <Typography variant="body2" color="text.secondary" component="div">
            Share this code with your staff. They should log in, go to{' '}
            <strong>&quot;Join a Shop&quot;</strong>, enter this code, and select their desired role.
            You will then be able to approve or reject their request from the{' '}
            <strong>Staff Requests</strong> page.
          </Typography>
        </Card>
      )}

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Regenerate Invite Code</DialogTitle>
        <DialogContent>
          <Typography>
            This will invalidate the old code. Any staff member who hasn&apos;t used it yet will no
            longer be able to join with it. Continue?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="warning"
            disabled={regenerate.isPending}
            onClick={() => regenerate.mutate()}
          >
            {regenerate.isPending ? 'Regenerating...' : 'Regenerate'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
