import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Chip, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert, MenuItem, Stack,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api from '../lib/api';
import { useCrudDialog } from '../lib/useCrudDialog';
import { useDefaultShopId } from '../stores/shopStore';
import type { PageResponse, Status, Supplier } from '../types';

const FIELDS = ['name', 'companyName', 'phone', 'email', 'address', 'panNumber'] as const;
const STATUSES: Status[] = ['ACTIVE', 'INACTIVE'];
type SupplierForm = Record<string, string>;

export default function SuppliersPage() {
  const shopId = useDefaultShopId();
  const [page, setPage] = useState(0);

  const { open, editing, form, setForm, error, setError, openNew, openEdit, close, save, remove } =
    useCrudDialog<Supplier, SupplierForm>({
      path: '/suppliers',
      invalidateKey: 'suppliers',
      emptyForm: { name: '', status: 'ACTIVE' },
      toForm: (s) => ({
        name: s.name,
        companyName: s.companyName ?? '',
        phone: s.phone ?? '',
        email: s.email ?? '',
        address: s.address ?? '',
        panNumber: s.panNumber ?? '',
        status: s.status,
      }),
      toPayload: (f, current) => {
        const payload: Record<string, unknown> = { shopId };
        FIELDS.forEach((k) => { payload[k] = f[k] || null; });
        if (current) payload.status = f.status;
        return payload;
      },
    });

  const { data, isLoading } = useQuery({
    queryKey: ['suppliers', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Supplier> }>('/suppliers', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Suppliers</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openNew}>
          New Supplier
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
                <TableCell>Company</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((s) => (
                <TableRow key={s.id}>
                  <TableCell>{s.name}</TableCell>
                  <TableCell>{s.companyName ?? '-'}</TableCell>
                  <TableCell>{s.phone ?? '-'}</TableCell>
                  <TableCell>{s.email ?? '-'}</TableCell>
                  <TableCell>
                    <Chip size="small" label={s.status} color={s.status === 'ACTIVE' ? 'success' : 'default'} />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" onClick={() => openEdit(s)}>
                        Edit
                      </Button>
                      <IconButton size="small" onClick={() => {
                        if (!window.confirm(`Deactivate supplier "${s.name}"?`)) return;
                        remove.mutate(s.id);
                      }}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No suppliers found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Supplier - ${editing.name}` : 'New Supplier'}</DialogTitle>
        <DialogContent>
          {FIELDS.map((f) => (
            <TextField
              key={f}
              label={f.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())}
              fullWidth
              margin="dense"
              value={form[f] ?? ''}
              onChange={(e) => setForm({ ...form, [f]: e.target.value })}
            />
          ))}
          {editing && (
            <TextField
              select
              label="Status"
              fullWidth
              margin="dense"
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value })}
            >
              {STATUSES.map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" disabled={!form.name || save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
