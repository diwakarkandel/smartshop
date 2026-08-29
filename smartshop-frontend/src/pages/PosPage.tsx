import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Grid,
  TextField,
  InputAdornment,
  Card,
  Typography,
  List,
  ListItem,
  ListItemText,
  Divider,
  Button,
  Chip,
  IconButton,
  CircularProgress,
  Alert,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import DeleteIcon from '@mui/icons-material/Delete';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import api, { extractErrorMessage } from '../lib/api';
import { computeTaxBreakdown, round2 } from '../lib/tax';
import TaxTotals from '../components/billing/TaxTotals';
import { defaultShopId, defaultBranchId } from '../stores/shopStore';
import type { ProductSearch, Sale } from '../types';

interface CartLine {
  product: ProductSearch;
  quantity: number;
}

export default function PosPage() {
  const shopId = defaultShopId();
  const branchId = defaultBranchId();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const [cart, setCart] = useState<CartLine[]>([]);
  const [tendered, setTendered] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const { data: results, isLoading } = useQuery({
    queryKey: ['pos-search', branchId, query],
    queryFn: async () => {
      const res = await api.get<{ data: ProductSearch[] }>('/products/search', {
        params: { branchId, query },
      });
      return res.data.data;
    },
    enabled: Boolean(branchId) && query.length > 0,
  });

  const addToCart = (p: ProductSearch) => {
    setCart((prev) => {
      const existing = prev.find((l) => l.product.id === p.id);
      if (existing) {
        return prev.map((l) =>
          l.product.id === p.id ? { ...l, quantity: round2(l.quantity + 1) } : l,
        );
      }
      return [...prev, { product: p, quantity: 1 }];
    });
  };

  const changeQty = (id: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((l) => (l.product.id === id ? { ...l, quantity: Math.max(0, round2(l.quantity + delta)) } : l))
        .filter((l) => l.quantity > 0),
    );
  };

  const removeLine = (id: string) => setCart((prev) => prev.filter((l) => l.product.id !== id));

  const breakdown = useMemo(
    () =>
      computeTaxBreakdown(
        cart.map((l) => ({
          quantity: l.quantity,
          unitPrice: l.product.sellingPrice,
          vatApplicable: l.product.vatApplicable,
          vatRate: l.product.vatRate,
        })),
      ),
    [cart],
  );

  const subtotal = breakdown.subtotal;
  const vat = breakdown.vatAmount;
  const total = breakdown.totalAmount;

  const change = tendered ? round2(Number(tendered) - total) : 0;

  const createSale = useMutation({
    mutationFn: async () => {
      const res = await api.post<{ data: Sale }>('/sales', {
        shopId,
        branchId,
        discountAmount: 0,
        paymentStatus: 'PAID',
        paymentMethod: 'CASH',
        cashTendered: Number(tendered),
        items: cart.map((l) => ({
          productId: l.product.id,
          quantity: l.quantity,
          unitPrice: l.product.sellingPrice,
          vatRate: l.product.vatRate,
        })),
      });
      return res.data.data;
    },
    onSuccess: (sale) => {
      setMessage(`Sale completed: ${sale.invoiceNumber}. Change: ${change.toFixed(2)}`);
      setCart([]);
      setTendered('');
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['pos-search'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Point of Sale
      </Typography>
      {message && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>
          {message}
        </Alert>
      )}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      <Grid container spacing={2}>
        <Grid item xs={12} md={8}>
          <Card sx={{ p: 2, height: 'calc(100vh - 160px)', overflow: 'auto' }}>
            <TextField
              fullWidth
              placeholder="Search by name, SKU or barcode..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              sx={{ mb: 2 }}
            />
            {isLoading && (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            )}
            {!isLoading && query.length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
                Type to search for products
              </Typography>
            )}
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell>Price</TableCell>
                  <TableCell>Stock</TableCell>
                  <TableCell></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(results ?? []).map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>
                      <Typography variant="body2">{p.name}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {p.sku}
                      </Typography>
                    </TableCell>
                    <TableCell>{p.sellingPrice.toFixed(2)}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        color={p.stockStatus === 'IN_STOCK' ? 'success' : 'warning'}
                        label={`${p.quantityAvailable} ${p.unit}`}
                      />
                    </TableCell>
                    <TableCell>
                      <IconButton
                        size="small"
                        onClick={() => addToCart(p)}
                        disabled={p.quantityAvailable <= 0}
                      >
                        <AddIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card sx={{ p: 2, height: 'calc(100vh - 160px)', display: 'flex', flexDirection: 'column' }}>
            <Typography variant="h6" sx={{ mb: 1 }}>
              Cart
            </Typography>
            <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
              <List dense>
                {cart.map((line) => (
                  <ListItem
                    key={line.product.id}
                    secondaryAction={
                      <IconButton edge="end" size="small" onClick={() => removeLine(line.product.id)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    }
                  >
                    <ListItemText
                      primary={line.product.name}
                      secondary={`${line.product.sellingPrice.toFixed(2)} x ${line.quantity}`}
                    />
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      <IconButton size="small" onClick={() => changeQty(line.product.id, -1)}>
                        <RemoveIcon fontSize="small" />
                      </IconButton>
                      <Typography variant="body2">{line.quantity}</Typography>
                      <IconButton size="small" onClick={() => changeQty(line.product.id, 1)}>
                        <AddIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </ListItem>
                ))}
              </List>
              {cart.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  Cart is empty
                </Typography>
              )}
            </Box>
            <Divider sx={{ my: 1 }} />
            <Box>
              <TaxTotals
                subtotal={subtotal}
                byRate={breakdown.byRate}
                tax={vat}
                total={total}
              />
              <TextField
                label="Cash tendered"
                type="number"
                fullWidth
                size="small"
                value={tendered}
                onChange={(e) => setTendered(e.target.value)}
                sx={{ mb: 1 }}
              />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="body2">Change</Typography>
                <Typography variant="body2" color={change < 0 ? 'error' : 'success'}>
                  {change.toFixed(2)}
                </Typography>
              </Box>
              <Button
                variant="contained"
                fullWidth
                size="large"
                startIcon={<PointOfSaleIcon />}
                disabled={cart.length === 0 || !tendered || Number(tendered) < total || createSale.isPending}
                onClick={() => createSale.mutate()}
              >
                {createSale.isPending ? <CircularProgress size={22} color="inherit" /> : 'Charge'}
              </Button>
            </Box>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}