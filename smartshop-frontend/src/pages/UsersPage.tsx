import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Chip, CircularProgress, Alert, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, MenuItem, Pagination,
} from '@mui/material';
import { useState } from 'react';
import AddIcon from '@mui/icons-material/Add';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { defaultShopId } from '../stores/shopStore';
import type { PageResponse } from '../types';

interface UserRow {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  status: string;
  roles: string[];
}

export default function UsersPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ email: '', firstName: '', lastName: '', password: '', role: 'MANAGER' });

  const { data, isLoading } = useQuery({
    queryKey: ['users', page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<UserRow> }>('/users', {
        params: { page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(user?.roles.includes('SUPER_ADMIN')),
  });

  const create = useMutation({
    mutationFn: async () =>
      api.post('/auth/register', {
        email: form.email,
        firstName: form.firstName,
        lastName: form.lastName,
        password: form.password,
        shopId,
        role: form.role,
      }),
    onSuccess: () => {
      setOpen(false);
      setForm({ email: '', firstName: '', lastName: '', password: '', role: 'MANAGER' });
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Users</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          Invite User
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
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Roles</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((u) => (
                <TableRow key={u.id}>
                  <TableCell>
                    {u.firstName} {u.lastName}
                  </TableCell>
                  <TableCell>{u.email}</TableCell>
                  <TableCell>
                    {(u.roles ?? []).map((r) => (
                      <Chip key={r} size="small" label={r} sx={{ mr: 0.5 }} />
                    ))}
                  </TableCell>
                  <TableCell>
                    <Chip size="small" color={u.status === 'ACTIVE' ? 'success' : 'default'} label={u.status} />
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>No users</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Invite User</DialogTitle>
        <DialogContent>
          <TextField label="First Name" fullWidth margin="dense" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
          <TextField label="Last Name" fullWidth margin="dense" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
          <TextField label="Email" fullWidth margin="dense" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <TextField label="Password" type="password" fullWidth margin="dense" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <TextField
            select
            label="Role"
            fullWidth
            margin="dense"
            value={form.role}
            onChange={(e) => setForm({ ...form, role: e.target.value })}
          >
            {['SHOP_ADMIN', 'MANAGER', 'CASHIER', 'INVENTORY_STAFF', 'ACCOUNTANT'].map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={create.isPending || !form.email} onClick={() => create.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}