import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField, MenuItem,
  IconButton, Alert, Chip, CircularProgress, Divider,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId } from '../stores/shopStore';
import type {
  ApiResponse, PageResponse, User, UserBranchRole, UserBranchRoleRequest,
  Branch, RoleRef,
} from '../types';

export default function RoleAssignmentsPage() {
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [assignOpen, setAssignOpen] = useState(false);
  const [form, setForm] = useState({ branchId: '', roleId: '' });

  const { data: users, isLoading: usersLoading } = useQuery({
    queryKey: ['users', search],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PageResponse<User>>>('/users', {
        params: { search: search || undefined, page: 0, size: 50 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const { data: assignments, isLoading: assignLoading } = useQuery({
    queryKey: ['user-branch-roles', selectedUser?.id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<UserBranchRole[]>>('/user-branch-roles', {
        params: { userId: selectedUser?.id },
      });
      return res.data.data;
    },
    enabled: Boolean(selectedUser?.id),
  });

  const { data: branches } = useQuery({
    queryKey: ['branches-simple', shopId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<Branch[]>>('/branches', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: roles } = useQuery({
    queryKey: ['roles'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<RoleRef[]>>('/roles');
      return res.data.data;
    },
  });

  const assign = useMutation({
    mutationFn: async () => {
      const payload: UserBranchRoleRequest = {
        userId: selectedUser!.id,
        shopId: shopId!,
        branchId: form.branchId || null,
        roleId: form.roleId,
      };
      await api.post('/user-branch-roles', payload);
    },
    onSuccess: () => {
      setAssignOpen(false);
      setForm({ branchId: '', roleId: '' });
      setError('');
      queryClient.invalidateQueries({ queryKey: ['user-branch-roles', selectedUser?.id] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const revoke = useMutation({
    mutationFn: async (id: string) => api.delete(`/user-branch-roles/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['user-branch-roles', selectedUser?.id] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box sx={{ display: 'flex', gap: 2 }}>
      {/* Users list */}
      <Card sx={{ width: 300, p: 2, flexShrink: 0 }}>
        <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>Users</Typography>
        <TextField
          size="small"
          fullWidth
          placeholder="Search users..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ mb: 1 }}
        />
        {usersLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress size={24} /></Box>
        ) : (
          <Box sx={{ maxHeight: 500, overflowY: 'auto' }}>
            {(users ?? []).map((u) => (
              <Box
                key={u.id}
                onClick={() => setSelectedUser(u)}
                sx={{
                  p: 1, borderRadius: 1, cursor: 'pointer',
                  bgcolor: selectedUser?.id === u.id ? 'action.selected' : 'transparent',
                  '&:hover': { bgcolor: 'action.hover' },
                  mb: 0.5,
                }}
              >
                <Typography variant="body2" fontWeight="medium">{u.fullName}</Typography>
                <Typography variant="caption" color="text.secondary">{u.email}</Typography>
              </Box>
            ))}
            {(users ?? []).length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ p: 1 }}>No users found.</Typography>
            )}
          </Box>
        )}
      </Card>

      {/* Assignments panel */}
      <Box sx={{ flex: 1 }}>
        {!selectedUser ? (
          <Card sx={{ p: 4, textAlign: 'center' }}>
            <Typography color="text.secondary">Select a user to manage their role assignments.</Typography>
          </Card>
        ) : (
          <Card sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Box>
                <Typography variant="h6">{selectedUser.fullName}</Typography>
                <Typography variant="body2" color="text.secondary">{selectedUser.email}</Typography>
              </Box>
              <Button variant="contained" onClick={() => setAssignOpen(true)}>
                Assign Role
              </Button>
            </Box>
            {error && <Alert severity="error" sx={{ mb: 1 }} onClose={() => setError('')}>{error}</Alert>}
            <Divider sx={{ mb: 1.5 }} />
            {assignLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress size={24} /></Box>
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Shop</TableCell>
                    <TableCell>Branch</TableCell>
                    <TableCell>Role</TableCell>
                    <TableCell>Active</TableCell>
                    <TableCell />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(assignments ?? []).map((a) => (
                    <TableRow key={a.id}>
                      <TableCell>{a.shopName}</TableCell>
                      <TableCell>{a.branchName ?? 'Shop-wide'}</TableCell>
                      <TableCell><Chip size="small" label={a.roleName} /></TableCell>
                      <TableCell>
                        <Chip size="small" color={a.isActive ? 'success' : 'default'} label={a.isActive ? 'Active' : 'Revoked'} />
                      </TableCell>
                      <TableCell>
                        {a.isActive && (
                          <IconButton size="small" color="error" onClick={() => revoke.mutate(a.id)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                  {(assignments ?? []).length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                        No role assignments found.
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            )}
          </Card>
        )}
      </Box>

      {/* Assign dialog */}
      <Dialog open={assignOpen} onClose={() => setAssignOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Assign Role to {selectedUser?.fullName}</DialogTitle>
        <DialogContent>
          <TextField
            select
            label="Branch (optional)"
            fullWidth
            size="small"
            margin="dense"
            value={form.branchId}
            onChange={(e) => setForm({ ...form, branchId: e.target.value })}
          >
            <MenuItem value="">Shop-wide (no branch)</MenuItem>
            {(branches ?? []).map((b) => (
              <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Role"
            fullWidth
            size="small"
            margin="dense"
            value={form.roleId}
            onChange={(e) => setForm({ ...form, roleId: e.target.value })}
          >
            {(roles ?? []).map((r) => (
              <MenuItem key={r.id} value={r.id}>{r.name}</MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssignOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.roleId || assign.isPending}
            onClick={() => assign.mutate()}
          >
            Assign
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
