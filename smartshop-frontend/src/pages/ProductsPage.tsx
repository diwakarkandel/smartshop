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
  Alert,
  MenuItem,
  Pagination,
  CircularProgress,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { defaultShopId } from '../stores/shopStore';
import type { PageResponse, Product } from '../types';

export default function ProductsPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    name: '',
    sku: '',
    barcode: '',
    brand: '',
    unit: 'PCS',
    purchasePrice: '0',
    sellingPrice: '0',
    vatApplicable: true,
    reorderLevel: '0',
  });

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

  const createProduct = useMutation({
    mutationFn: async () => {
      const res = await api.post<{ data: Product }>('/products', {
        shopId,
        name: form.name,
        sku: form.sku,
        barcode: form.barcode || null,
        brand: form.brand || null,
        unit: form.unit,
        purchasePrice: Number(form.purchasePrice),
        sellingPrice: Number(form.sellingPrice),
        vatApplicable: form.vatApplicable,
        reorderLevel: Number(form.reorderLevel),
      });
      return res.data.data;
    },
    onSuccess: () => {
      setOpen(false);
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
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
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
                <TableCell>VAT</TableCell>
                <TableCell>Status</TableCell>
                <TableCell></TableCell>
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
                  <TableCell>{p.vatApplicable ? `${p.vatRate}%` : '-'}</TableCell>
                  <TableCell>
                    <Chip size="small" color={p.status === 'ACTIVE' ? 'success' : 'default'} label={p.status} />
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => deactivate.mutate(p.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={8}>No products found</TableCell>
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
        <DialogTitle>New Product</DialogTitle>
        <DialogContent>
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
          <TextField label="Reorder Level" type="number" fullWidth margin="dense" value={form.reorderLevel} onChange={(e) => setForm({ ...form, reorderLevel: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!form.name || !form.sku || createProduct.isPending}
            onClick={() => createProduct.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}