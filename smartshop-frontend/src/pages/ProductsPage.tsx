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
  Stack,
  Alert,
  MenuItem,
  Pagination,
  CircularProgress,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId } from '../stores/shopStore';
import ImageUpload from '../components/common/ImageUpload';
import type { ApiResponse, PageResponse, Product, ProductRequest, Status } from '../types';

const STATUSES: Status[] = ['ACTIVE', 'INACTIVE', 'DISCONTINUED'];

interface ProductForm {
  name: string;
  sku: string;
  barcode: string;
  brand: string;
  unit: string;
  purchasePrice: string;
  sellingPrice: string;
  targetMargin: string;
  vatApplicable: boolean;
  reorderLevel: string;
  imageUrl: string;
  status: Status;
}

const EMPTY_FORM: ProductForm = {
  name: '',
  sku: '',
  barcode: '',
  brand: '',
  unit: 'PCS',
  purchasePrice: '0',
  sellingPrice: '0',
  targetMargin: '20',
  vatApplicable: true,
  reorderLevel: '0',
  imageUrl: '',
  status: 'ACTIVE',
};

export default function ProductsPage() {
  const shopId = useDefaultShopId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Product | null>(null);
  const [form, setForm] = useState<ProductForm>(EMPTY_FORM);
  const [suggesting, setSuggesting] = useState(false);

  const applyTargetMargin = async () => {
    const margin = Number(form.targetMargin) || 0;
    // For an existing product we can ask the backend, which prices off the real
    // weighted-average landed cost (not just the typed purchase price).
    if (editing) {
      try {
        setSuggesting(true);
        const res = await api.get<ApiResponse<{ suggestedSellingPrice: number }>>('/pricing/suggest', {
          params: { productId: editing.id, margin },
        });
        setForm((f) => ({ ...f, sellingPrice: Number(res.data.data.suggestedSellingPrice).toFixed(2) }));
        return;
      } catch (err) {
        setError(extractErrorMessage(err));
      } finally {
        setSuggesting(false);
      }
    }
    // New product: no cost history yet, fall back to a local calc from purchase price.
    const cost = Number(form.purchasePrice) || 0;
    setForm((f) => ({ ...f, sellingPrice: (cost * (1 + margin / 100)).toFixed(2) }));
  };

  const { data, isLoading } = useQuery({
    queryKey: ['products', shopId, page, search],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Product> }>('/products', {
        params: { shopId, page, size: 10, search: search || undefined },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const openNew = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setOpen(true);
  };

  const openEdit = (p: Product) => {
    setEditing(p);
    // PUT is a full replace of ProductRequest, so prefill every editable field.
    setForm({
      name: p.name,
      sku: p.sku,
      barcode: p.barcode ?? '',
      brand: p.brand ?? '',
      unit: p.unit,
      purchasePrice: String(p.purchasePrice ?? 0),
      sellingPrice: String(p.sellingPrice ?? 0),
      targetMargin: '20',
      vatApplicable: p.vatApplicable,
      reorderLevel: String(p.reorderLevel ?? 0),
      imageUrl: p.imageUrl ?? '',
      status: p.status,
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
      const payload: ProductRequest = {
        shopId: shopId ?? '',
        name: form.name,
        sku: form.sku,
        barcode: form.barcode || null,
        brand: form.brand || null,
        unit: form.unit,
        purchasePrice: Number(form.purchasePrice),
        sellingPrice: Number(form.sellingPrice),
        vatApplicable: form.vatApplicable,
        reorderLevel: Number(form.reorderLevel),
        imageUrl: form.imageUrl || null,
        status: form.status,
      };
      return editing
        ? api.put<{ data: Product }>(`/products/${editing.id}`, payload)
        : api.post<{ data: Product }>('/products', payload);
    },
    onSuccess: () => {
      closeDialog();
      setError('');
      queryClient.invalidateQueries({ queryKey: ['products'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const deactivate = useMutation({
    mutationFn: async (id: string) => api.delete(`/products/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['products'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Products</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <TextField
            size="small"
            placeholder="Search products"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
          <Button variant="contained" startIcon={<AddIcon />} onClick={openNew}>
            New Product
          </Button>
        </Box>
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
                <TableCell>SKU</TableCell>
                <TableCell>Brand</TableCell>
                <TableCell align="right">Purchase</TableCell>
                <TableCell align="right">Selling</TableCell>
                <TableCell align="right">Suggested</TableCell>
                <TableCell align="right">Margin %</TableCell>
                <TableCell>VAT</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.name}</TableCell>
                  <TableCell>{p.sku}</TableCell>
                  <TableCell>{p.brand ?? '-'}</TableCell>
                  <TableCell align="right">{p.purchasePrice.toFixed(2)}</TableCell>
                  <TableCell align="right">{p.sellingPrice.toFixed(2)}</TableCell>
                  <TableCell align="right">
                    {p.suggestedSellingPrice !== undefined ? p.suggestedSellingPrice.toFixed(2) : '-'}
                  </TableCell>
                  <TableCell align="right">
                    {p.profitMarginPercent !== undefined ? `${p.profitMarginPercent.toFixed(1)}%` : '-'}
                  </TableCell>
                  <TableCell>{p.vatApplicable ? `${p.vatRate}%` : '-'}</TableCell>
                  <TableCell>
                    <Chip size="small" color={p.status === 'ACTIVE' ? 'success' : 'default'} label={p.status} />
                  </TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" onClick={() => openEdit(p)}>
                        Edit
                      </Button>
                      <Button
                        size="small"
                        color="error"
                        disabled={p.status === 'INACTIVE' || deactivate.isPending}
                        onClick={() => {
                          if (!window.confirm(`Deactivate product "${p.name}"?`)) return;
                          deactivate.mutate(p.id);
                        }}
                      >
                        Deactivate
                      </Button>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={10}>No products found</TableCell>
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

      <Dialog open={open} onClose={closeDialog} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Product - ${editing.name}` : 'New Product'}</DialogTitle>
        <DialogContent>
          <Box sx={{ mb: 2 }}>
            <ImageUpload
              value={form.imageUrl}
              onChange={(url) => setForm({ ...form, imageUrl: url || '' })}
              label="Product Image"
              size={100}
            />
          </Box>
          <TextField label="Name" fullWidth margin="dense" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <TextField label="SKU" fullWidth margin="dense" value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} />
          <TextField label="Barcode" fullWidth margin="dense" value={form.barcode} onChange={(e) => setForm({ ...form, barcode: e.target.value })} />
          <TextField label="Brand" fullWidth margin="dense" value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} />
          <TextField
            select
            label="Unit"
            fullWidth
            margin="dense"
            value={form.unit}
            onChange={(e) => setForm({ ...form, unit: e.target.value })}
          >
            {['PCS', 'KG', 'LTR', 'BOX', 'PACK', 'DOZEN'].map((u) => (
              <MenuItem key={u} value={u}>
                {u}
              </MenuItem>
            ))}
          </TextField>
          <TextField label="Purchase Price" type="number" fullWidth margin="dense" value={form.purchasePrice} onChange={(e) => setForm({ ...form, purchasePrice: e.target.value })} />
          <TextField label="Selling Price" type="number" fullWidth margin="dense" value={form.sellingPrice} onChange={(e) => setForm({ ...form, sellingPrice: e.target.value })} />
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <TextField
              label="Target Margin %"
              type="number"
              size="small"
              margin="dense"
              sx={{ flex: 1 }}
              value={form.targetMargin}
              onChange={(e) => setForm({ ...form, targetMargin: e.target.value })}
            />
            <Button size="small" variant="outlined" sx={{ mt: 1 }} disabled={suggesting} onClick={applyTargetMargin}>
              {editing ? 'Suggest Price' : 'Auto-fill Price'}
            </Button>
          </Box>
          {editing && (
            <Typography variant="caption" color="text.secondary" display="block">
              Suggest uses this product&apos;s real weighted-average cost from inventory history.
            </Typography>
          )}
          <TextField label="Reorder Level" type="number" fullWidth margin="dense" value={form.reorderLevel} onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })} />
          {editing && (
            <TextField
              select
              label="Status"
              fullWidth
              margin="dense"
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value as Status })}
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
          <Button onClick={closeDialog}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.name || !form.sku || save.isPending}
            onClick={() => save.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
