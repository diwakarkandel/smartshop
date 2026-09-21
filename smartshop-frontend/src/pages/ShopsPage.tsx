import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Card,
  Typography,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Pagination,
  CircularProgress,
  Alert,
  MenuItem,
  Stack,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import BlockIcon from '@mui/icons-material/Block';
import api, { extractErrorMessage } from '../lib/api';
import Can from '../components/guards/Can';
import { ROLES } from '../lib/routeRoles';
import type { PageResponse, Shop, ShopRequest, ShopStatus } from '../types';

/**
 * `GET /shops` feeds `sort` straight into an unvalidated `sort.split(",")`, so an
 * unknown column name comes back as a 500. Only the four whitelisted pairs below
 * are ever sent to the server.
 */
const SORT_OPTIONS = [
  { value: 'name,asc', label: 'Name (A-Z)' },
  { value: 'name,desc', label: 'Name (Z-A)' },
  { value: 'createdAt,desc', label: 'Newest first' },
  { value: 'status,asc', label: 'Status' },
] as const;

type SortOption = (typeof SORT_OPTIONS)[number]['value'];

const SHOP_STATUSES: ShopStatus[] = ['ACTIVE', 'INACTIVE', 'SUSPENDED'];

const STATUS_COLOR: Record<ShopStatus, 'success' | 'warning' | 'default'> = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  INACTIVE: 'default',
};

interface ShopForm {
  name: string;
  panVatNumber: string;
  phone: string;
  email: string;
  address: string;
  logoUrl: string;
  status: ShopStatus;
}

const EMPTY_FORM: ShopForm = {
  name: '',
  panVatNumber: '',
  phone: '',
  email: '',
  address: '',
  logoUrl: '',
  status: 'ACTIVE',
};

/** Optional columns arrive as absent keys, never `null` — coalesce to '' for the inputs. */
const toForm = (shop: Shop): ShopForm => ({
  name: shop.name,
  panVatNumber: shop.panVatNumber,
  phone: shop.phone ?? '',
  email: shop.email ?? '',
  address: shop.address ?? '',
  logoUrl: shop.logoUrl ?? '',
  status: shop.status,
});

/**
 * `ShopService.applyRequest()` assigns every scalar with no null check, so `PUT` is a
 * FULL REPLACE and not a patch: any key we leave out is wiped on the server. The edit
 * dialog is therefore always seeded from the row and always resubmits every field —
 * and a box the user deliberately empties is meant to be cleared.
 */
const buildPayload = (form: ShopForm, includeStatus: boolean): ShopRequest => ({
  name: form.name.trim(),
  panVatNumber: form.panVatNumber.trim(),
  phone: form.phone.trim() || undefined,
  email: form.email.trim() || undefined,
  address: form.address.trim() || undefined,
  logoUrl: form.logoUrl.trim() || undefined,
  // On create the server picks the default status; the selector only shows when editing.
  status: includeStatus ? form.status : undefined,
});

export default function ShopsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<SortOption>('name,asc');
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Shop | null>(null);
  const [form, setForm] = useState<ShopForm>(EMPTY_FORM);

  // Platform-wide list: this page is NOT shop-scoped, so there is no shopId param.
  const { data, isLoading, isError, error: loadError } = useQuery({
    queryKey: ['shops', page, search, sort],
    queryFn: async () => {
      const term = search.trim();
      const res = await api.get<{ data: PageResponse<Shop> }>('/shops', {
        params: {
          page,
          size: 10,
          sort,
          // `search` is optional free text — omit the param entirely when the box is empty.
          ...(term ? { search: term } : {}),
        },
      });
      return res.data.data;
    },
  });

  const save = useMutation({
    mutationFn: async () => {
      const payload = buildPayload(form, Boolean(editing));
      const res = editing
        ? await api.put<{ data: Shop }>(`/shops/${editing.id}`, payload)
        : await api.post<{ data: Shop }>('/shops', payload);
      return res.data.data;
    },
    onSuccess: (saved) => {
      // Re-seed from the response: text is trimmed/normalised server-side, so the
      // saved row is authoritative for the next edit of this shop.
      setEditing(saved);
      setForm(toForm(saved));
      setError('');
      setOpen(false);
      queryClient.invalidateQueries({ queryKey: ['shops'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  // Soft delete: the server flips status to INACTIVE and returns no `data` key.
  const deactivate = useMutation({
    mutationFn: async (id: string) => api.delete(`/shops/${id}`),
    onSuccess: () => {
      setError('');
      queryClient.invalidateQueries({ queryKey: ['shops'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const openNew = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setError('');
    setOpen(true);
  };

  const openEdit = (shop: Shop) => {
    setEditing(shop);
    setForm(toForm(shop));
    setError('');
    setOpen(true);
  };

  const confirmDeactivate = (shop: Shop) => {
    const ok = window.confirm(
      `Deactivate shop "${shop.name}"? This is a soft delete: the shop is set to INACTIVE ` +
        'and stays in the list. You can reactivate it later by editing its status.',
    );
    if (ok) deactivate.mutate(shop.id);
  };

  const rows = data?.content ?? [];
  const canSave = Boolean(form.name.trim()) && Boolean(form.panVatNumber.trim());

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: 2,
          flexWrap: 'wrap',
          mb: 2,
        }}
      >
        <Typography variant="h5">Shops</Typography>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            size="small"
            label="Search"
            placeholder="Name, PAN/VAT, phone"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
          <TextField
            select
            size="small"
            label="Sort"
            value={sort}
            sx={{ minWidth: 170 }}
            onChange={(e) => {
              setSort(e.target.value as SortOption);
              setPage(0);
            }}
          >
            {SORT_OPTIONS.map((o) => (
              <MenuItem key={o.value} value={o.value}>
                {o.label}
              </MenuItem>
            ))}
          </TextField>
          <Can roles={[ROLES.SUPER_ADMIN]}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={openNew}>
              New Shop
            </Button>
          </Can>
        </Box>
      </Box>

      {/* Shown only while the dialog is shut — an open modal would hide it behind the backdrop. */}
      {error && !open && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Could not load shops: {extractErrorMessage(loadError)}
        </Alert>
      )}
      <Can
        roles={[ROLES.SUPER_ADMIN]}
        fallback={
          <Alert severity="info" sx={{ mb: 2 }}>
            You have read-only access to shops. Only a platform super admin can add, edit or
            deactivate one.
          </Alert>
        }
      >
        {null}
      </Can>

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
                <TableCell>PAN/VAT</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Address</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((s) => (
                <TableRow key={s.id}>
                  <TableCell>{s.name}</TableCell>
                  <TableCell>{s.panVatNumber}</TableCell>
                  <TableCell>{s.phone ?? '-'}</TableCell>
                  <TableCell>{s.email ?? '-'}</TableCell>
                  <TableCell>{s.address ?? '-'}</TableCell>
                  <TableCell>
                    <Chip size="small" color={STATUS_COLOR[s.status]} label={s.status} />
                  </TableCell>
                  <TableCell align="right">
                    <Can roles={[ROLES.SUPER_ADMIN]}>
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end" alignItems="center">
                        <IconButton size="small" aria-label="Edit shop" onClick={() => openEdit(s)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <Button
                          size="small"
                          color="warning"
                          startIcon={<BlockIcon fontSize="small" />}
                          disabled={s.status === 'INACTIVE' || deactivate.isPending}
                          onClick={() => confirmDeactivate(s)}
                        >
                          Deactivate
                        </Button>
                      </Stack>
                    </Can>
                  </TableCell>
                </TableRow>
              ))}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>
                    {search.trim()
                      ? `No shops match "${search.trim()}". Clear the search box to see every shop.`
                      : 'No shops registered yet. Create one to start onboarding branches and staff.'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination
          count={data?.totalPages ?? 1}
          page={page + 1}
          onChange={(_, p) => setPage(p - 1)}
        />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Shop - ${editing.name}` : 'New Shop'}</DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
              {error}
            </Alert>
          )}
          {editing && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Saving replaces the whole shop record. Every field below is submitted, so anything
              you clear here is cleared on the server too.
            </Alert>
          )}
          <TextField
            label="Name"
            required
            fullWidth
            margin="dense"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
          <TextField
            label="PAN/VAT Number"
            required
            fullWidth
            margin="dense"
            value={form.panVatNumber}
            helperText="Must be unique across every shop on the platform."
            onChange={(e) => setForm({ ...form, panVatNumber: e.target.value })}
          />
          <TextField
            label="Phone"
            fullWidth
            margin="dense"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
          />
          <TextField
            label="Email"
            type="email"
            fullWidth
            margin="dense"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
          <TextField
            label="Address"
            fullWidth
            margin="dense"
            multiline
            minRows={2}
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
          />
          <TextField
            label="Logo URL"
            fullWidth
            margin="dense"
            value={form.logoUrl}
            onChange={(e) => setForm({ ...form, logoUrl: e.target.value })}
          />
          {editing && (
            <TextField
              select
              label="Status"
              fullWidth
              margin="dense"
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value as ShopStatus })}
            >
              {SHOP_STATUSES.map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!canSave || save.isPending}
            onClick={() => save.mutate()}
          >
            {save.isPending ? <CircularProgress size={20} color="inherit" /> : 'Save'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
