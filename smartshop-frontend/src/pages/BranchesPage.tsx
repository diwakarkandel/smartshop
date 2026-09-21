import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  MenuItem, Chip, CircularProgress, Alert, Stack, Checkbox, FormControlLabel,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId } from '../stores/shopStore';
import Can from '../components/guards/Can';
import { ROLES } from '../lib/routeRoles';
import type { Branch, BranchRequest, BranchStatus } from '../types';

/** Branches only toggle between these two — there is no SUSPENDED. */
const BRANCH_STATUSES: BranchStatus[] = ['ACTIVE', 'INACTIVE'];

interface BranchForm {
  name: string;
  code: string;
  address: string;
  contactNumber: string;
  isMainBranch: boolean;
  status: BranchStatus;
}

const EMPTY_FORM: BranchForm = {
  name: '',
  code: '',
  address: '',
  contactNumber: '',
  isMainBranch: false,
  status: 'ACTIVE',
};

export default function BranchesPage() {
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Branch | null>(null);
  const [form, setForm] = useState<BranchForm>(EMPTY_FORM);

  // GET /branches is NOT paged — it returns a plain array already sorted by name ASC.
  const { data, isLoading } = useQuery({
    queryKey: ['branches', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', { params: { shopId } });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const branches = data ?? [];
  // Every row carries the same shopName; use the first one as a header subtitle.
  const shopName = branches[0]?.shopName;

  const openNew = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setOpen(true);
  };

  const openEdit = (branch: Branch) => {
    setEditing(branch);
    // PUT is a FULL REPLACE of BranchRequest, so prefill every editable field —
    // anything left out of the payload would be wiped rather than preserved.
    setForm({
      name: branch.name,
      code: branch.code,
      address: branch.address ?? '',
      contactNumber: branch.contactNumber ?? '',
      isMainBranch: branch.isMainBranch,
      status: branch.status,
    });
    setOpen(true);
  };

  const closeDialog = () => {
    setOpen(false);
    setEditing(null);
    setForm(EMPTY_FORM);
  };

  const save = useMutation({
    mutationFn: async () => {
      const payload: BranchRequest = {
        // shopId is @NotNull on BranchRequest so it must be sent on update too,
        // even though the server ignores it there and keeps the existing shop.
        shopId: shopId ?? '',
        name: form.name.trim(),
        // The server uppercases `code`; we mirror that in the input so the stored
        // value never differs from what was typed.
        code: form.code.trim().toUpperCase(),
        address: form.address.trim() || undefined,
        contactNumber: form.contactNumber.trim() || undefined,
        // On PUT, omitting status/isMainBranch would mean "leave unchanged"; the
        // form is always fully prefilled so we just send real values every time.
        isMainBranch: form.isMainBranch,
        status: form.status,
      };
      return editing
        ? api.put(`/branches/${editing.id}`, payload)
        : api.post('/branches', payload);
    },
    onSuccess: () => {
      closeDialog();
      // isMainBranch=true demotes every other branch in the shop, so the whole
      // list is stale after a write — never patch a single row.
      queryClient.invalidateQueries({ queryKey: ['branches'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const deactivate = useMutation({
    // DELETE is a soft delete: the branch comes back with status=INACTIVE.
    mutationFn: (id: string) => api.delete(`/branches/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['branches'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const canSubmit = Boolean(shopId) && Boolean(form.name.trim()) && Boolean(form.code.trim());

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h5">Branches</Typography>
          {shopName && (
            <Typography variant="body2" color="text.secondary">
              {shopName}
            </Typography>
          )}
        </Box>
        <Can roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]}>
          <Button variant="contained" startIcon={<AddIcon />} disabled={!shopId} onClick={openNew}>
            New Branch
          </Button>
        </Can>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {!shopId ? (
        <Alert severity="info">Select a shop from the top bar to manage its branches.</Alert>
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
                  <TableCell>Code</TableCell>
                  <TableCell>Address</TableCell>
                  <TableCell>Contact</TableCell>
                  <TableCell>Main</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right"></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {branches.map((b) => (
                  <TableRow key={b.id}>
                    <TableCell>{b.name}</TableCell>
                    <TableCell>{b.code}</TableCell>
                    <TableCell>{b.address ?? '-'}</TableCell>
                    <TableCell>{b.contactNumber ?? '-'}</TableCell>
                    <TableCell>
                      {b.isMainBranch ? <Chip size="small" color="primary" label="MAIN" /> : '-'}
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        color={b.status === 'ACTIVE' ? 'success' : 'default'}
                        label={b.status}
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Can roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]}>
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button size="small" onClick={() => openEdit(b)}>
                            Edit
                          </Button>
                          <Button
                            size="small"
                            color="error"
                            disabled={b.status === 'INACTIVE' || deactivate.isPending}
                            onClick={() => {
                              if (!window.confirm(`Deactivate branch "${b.name}"?`)) return;
                              deactivate.mutate(b.id);
                            }}
                          >
                            Deactivate
                          </Button>
                        </Stack>
                      </Can>
                    </TableCell>
                  </TableRow>
                ))}
                {branches.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7}>
                      No branches yet for this shop. Create one so sales, purchases and stock can
                      be recorded against it.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          )}
        </Card>
      )}

      <Dialog open={open} onClose={closeDialog} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Branch - ${editing.name}` : 'New Branch'}</DialogTitle>
        <DialogContent>
          <TextField
            label="Name"
            fullWidth
            margin="dense"
            required
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
          <TextField
            label="Code"
            fullWidth
            margin="dense"
            required
            value={form.code}
            // Uppercased here because the server uppercases it anyway.
            onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
            helperText="Stored uppercase and must be unique within the shop."
          />
          <TextField
            label="Address"
            fullWidth
            margin="dense"
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
          />
          <TextField
            label="Contact Number"
            fullWidth
            margin="dense"
            value={form.contactNumber}
            onChange={(e) => setForm({ ...form, contactNumber: e.target.value })}
          />
          <TextField
            label="Status"
            select
            fullWidth
            margin="dense"
            value={form.status}
            onChange={(e) => setForm({ ...form, status: e.target.value as BranchStatus })}
          >
            {BRANCH_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>
          <FormControlLabel
            sx={{ mt: 1 }}
            control={
              <Checkbox
                checked={form.isMainBranch}
                onChange={(e) => setForm({ ...form, isMainBranch: e.target.checked })}
              />
            }
            label="Main branch"
          />
          <Typography variant="caption" color="text.secondary" display="block">
            A shop has one main branch — setting this will demote the branch that currently holds
            the flag.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!canSubmit || save.isPending}
            onClick={() => save.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
