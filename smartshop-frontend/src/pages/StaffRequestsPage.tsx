import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Chip, CircularProgress, Alert, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, MenuItem,
} from '@mui/material';
import { useState } from 'react';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { useDefaultShopId } from '../stores/shopStore';
import type { ApiResponse, Branch, StaffInvitation } from '../types';

const ASSIGNABLE_ROLES = ['CASHIER', 'MANAGER', 'INVENTORY_STAFF'];

function roleLabel(role: string): string {
  if (role === 'INVENTORY_STAFF') return 'Inventory Staff';
  return role;
}

export default function StaffRequestsPage() {
  const hasRole = useAuthStore((s) => s.hasRole);
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [approveTarget, setApproveTarget] = useState<StaffInvitation | null>(null);
  const [approveBranchId, setApproveBranchId] = useState('');
  const [approveRole, setApproveRole] = useState('CASHIER');
  const [rejectTarget, setRejectTarget] = useState<StaffInvitation | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const { data: requests, isLoading } = useQuery({
    queryKey: ['staff-requests', shopId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<StaffInvitation[]>>('/staff-invitations/pending', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: hasRole('SHOP_ADMIN') && Boolean(shopId),
  });

  const { data: branches } = useQuery({
    queryKey: ['branches', shopId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Branch[]>>('/branches', { params: { shopId } });
      return res.data.data;
    },
    enabled: hasRole('SHOP_ADMIN') && Boolean(shopId),
  });

  const approve = useMutation({
    mutationFn: async ({ id, branchId, role }: { id: string; branchId: string | null; role: string }) =>
      api.post(`/staff-invitations/${id}/approve`, { branchId, role }),
    onSuccess: () => {
      setApproveTarget(null);
      setSuccess('Join request approved. The staff member has been notified via email.');
      queryClient.invalidateQueries({ queryKey: ['staff-requests'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const reject = useMutation({
    mutationFn: async ({ id, rejectionReason }: { id: string; rejectionReason: string }) =>
      api.post(`/staff-invitations/${id}/reject`, { rejectionReason }),
    onSuccess: () => {
      setRejectTarget(null);
      setRejectReason('');
      setSuccess('Join request rejected. The staff member has been notified via email.');
      queryClient.invalidateQueries({ queryKey: ['staff-requests'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  if (!hasRole('SHOP_ADMIN')) {
    return <Alert severity="error">Access Denied. This page is for Shop Admins only.</Alert>;
  }

  const openApproveDialog = (request: StaffInvitation) => {
    setApproveTarget(request);
    setApproveBranchId('');
    setApproveRole(ASSIGNABLE_ROLES.includes(request.requestedRole) ? request.requestedRole : 'CASHIER');
  };

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Staff Join Requests
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
      ) : (
        <Card>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Requested Role</TableCell>
                  <TableCell>Applied Date</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(requests ?? []).map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>{r.applicantName}</TableCell>
                    <TableCell>{r.applicantEmail}</TableCell>
                    <TableCell>{roleLabel(r.requestedRole)}</TableCell>
                    <TableCell>{new Date(r.createdAt).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        color={r.status === 'PENDING' ? 'warning' : r.status === 'APPROVED' ? 'success' : 'error'}
                        label={r.status}
                      />
                    </TableCell>
                    <TableCell>
                      {r.status === 'PENDING' ? (
                        <Box sx={{ display: 'flex', gap: 1 }}>
                          <Button
                            size="small"
                            variant="contained"
                            color="success"
                            onClick={() => openApproveDialog(r)}
                          >
                            Approve
                          </Button>
                          <Button
                            size="small"
                            variant="contained"
                            color="error"
                            onClick={() => setRejectTarget(r)}
                          >
                            Reject
                          </Button>
                        </Box>
                      ) : (
                        '-'
                      )}
                    </TableCell>
                  </TableRow>
                ))}
                {(requests ?? []).length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6}>No pending join requests</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </Card>
      )}

      <Dialog
        open={Boolean(approveTarget)}
        onClose={() => setApproveTarget(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Approve Join Request</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Approve <strong>{approveTarget?.applicantName}</strong> into your shop?
          </Typography>
          <TextField
            select
            label="Branch (optional)"
            fullWidth
            margin="dense"
            value={approveBranchId}
            onChange={(e) => setApproveBranchId(e.target.value)}
          >
            <MenuItem value="">No branch</MenuItem>
            {(branches ?? []).map((b) => (
              <MenuItem key={b.id} value={b.id}>
                {b.name}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Role"
            required
            fullWidth
            margin="dense"
            value={approveRole}
            onChange={(e) => setApproveRole(e.target.value)}
          >
            {ASSIGNABLE_ROLES.map((r) => (
              <MenuItem key={r} value={r}>
                {roleLabel(r)}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="success"
            disabled={approve.isPending || !approveRole}
            onClick={() =>
              approveTarget &&
              approve.mutate({ id: approveTarget.id, branchId: approveBranchId || null, role: approveRole })
            }
          >
            {approve.isPending ? 'Approving...' : 'Approve'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(rejectTarget)}
        onClose={() => setRejectTarget(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Reject Join Request</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Reject join request from <strong>{rejectTarget?.applicantName}</strong>?
          </Typography>
          <TextField
            label="Rejection Reason"
            required
            fullWidth
            multiline
            rows={3}
            margin="dense"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            disabled={reject.isPending || !rejectReason.trim()}
            onClick={() =>
              rejectTarget && reject.mutate({ id: rejectTarget.id, rejectionReason: rejectReason.trim() })
            }
          >
            {reject.isPending ? 'Rejecting...' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
