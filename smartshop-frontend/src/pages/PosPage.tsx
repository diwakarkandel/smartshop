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
  Autocomplete,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import DeleteIcon from '@mui/icons-material/Delete';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import PersonSearchIcon from '@mui/icons-material/PersonSearch';
import api, { extractErrorMessage } from '../lib/api';
import { computeTaxBreakdown, round2 } from '../lib/tax';
import TaxTotals from '../components/billing/TaxTotals';
import { useDefaultShopId, useDefaultBranchId, useShopStore } from '../stores/shopStore';
import type { Branch, Customer, PageResponse, ProductSearch, Sale } from '../types';

interface CartLine {
  product: ProductSearch;
  quantity: number;
}

export default function PosPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const setBranch = useShopStore((s) => s.setBranch);
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const [cart, setCart] = useState<CartLine[]>([]);
  const [tendered, setTendered] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  // Customer selection
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [quickCustomerOpen, setQuickCustomerOpen] = useState(false);
  const [quickName, setQuickName] = useState('');
  const [quickPhone, setQuickPhone] = useState('');

  // Branches list for direct branch switching in POS
  const { data: branches } = useQuery({
    queryKey: ['branches-pos', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  // Customers list for customer search by phone / name
  const { data: customers } = useQuery({
    queryKey: ['customers-pos', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Customer> }>('/customers', {
        params: { shopId, page: 0, size: 200 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const quickAddCustomer = useMutation({
    mutationFn: async () => {
      const res = await api.post<{ data: Customer }>('/customers', {
        shopId,
        name: quickName.trim(),
        phone: quickPhone.trim() || undefined,
      });
      return res.data.data;
    },
    onSuccess: (newCust) => {
      queryClient.invalidateQueries({ queryKey: ['customers-pos'] });
      setSelectedCustomer(newCust);
      setQuickCustomerOpen(false);
      setQuickName('');
      setQuickPhone('');
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

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
        customerId: selectedCustomer ? selectedCustomer.id : undefined,
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
      setMessage(`Sale completed: ${sale.invoiceNumber}. Change: ${change.toFixed(2)}${selectedCustomer ? ` (Customer: ${selectedCustomer.name})` : ''}`);
      setCart([]);
      setTendered('');
      setSelectedCustomer(null);
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['pos-search'] });
      queryClient.invalidateQueries({ queryKey: ['sales'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h5">Point of Sale</Typography>
          {(branches ?? []).length > 0 && (
            <TextField
              select
              size="small"
              label="Selling Branch"
              value={branchId ?? ''}
              onChange={(e) => setBranch(e.target.value || null)}
              sx={{ minWidth: 200 }}
            >
              {branches?.map((b) => (
                <MenuItem key={b.id} value={b.id}>
                  {b.name} ({b.code})
                </MenuItem>
              ))}
            </TextField>
          )}
        </Box>
      </Box>
      {!branchId && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          No branch is selected. Sales are recorded against a branch — create one under{' '}
          <strong>Administration → Branches</strong> and select it from the top bar to start selling.
        </Alert>
      )}
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
            {/* Customer Search & Selection */}
            <Box sx={{ mb: 1.5, p: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 1.5, bgcolor: 'action.hover' }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                <Typography variant="caption" sx={{ fontWeight: 700, textTransform: 'uppercase', color: 'text.secondary' }}>
                  Customer
                </Typography>
                <Button size="small" sx={{ fontSize: 11, py: 0, minWidth: 0 }} onClick={() => setQuickCustomerOpen(true)}>
                  + New Customer
                </Button>
              </Box>
              {selectedCustomer ? (
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', bgcolor: 'background.paper', p: 1, borderRadius: 1, border: '1px solid', borderColor: 'primary.light' }}>
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 700 }}>{selectedCustomer.name}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {selectedCustomer.phone ? `Phone: ${selectedCustomer.phone}` : 'No phone number'}
                    </Typography>
                  </Box>
                  <IconButton size="small" onClick={() => setSelectedCustomer(null)} title="Clear customer">
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Box>
              ) : (
                <Autocomplete
                  size="small"
                  options={customers ?? []}
                  getOptionLabel={(option) => `${option.name} ${option.phone ? `(${option.phone})` : ''}`}
                  filterOptions={(options, { inputValue }) => {
                    const term = inputValue.toLowerCase().trim();
                    return options.filter((c) =>
                      c.name.toLowerCase().includes(term) ||
                      (c.phone && c.phone.includes(term)) ||
                      (c.email && c.email.toLowerCase().includes(term))
                    );
                  }}
                  value={null}
                  onChange={(_, val) => setSelectedCustomer(val)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      placeholder="Search by phone or name..."
                      InputProps={{
                        ...params.InputProps,
                        startAdornment: (
                          <InputAdornment position="start">
                            <PersonSearchIcon fontSize="small" color="action" />
                          </InputAdornment>
                        ),
                      }}
                    />
                  )}
                />
              )}
            </Box>

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

      {/* Quick Add Customer Dialog */}
      <Dialog open={quickCustomerOpen} onClose={() => setQuickCustomerOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Quick Add Customer</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            required
            fullWidth
            margin="dense"
            label="Customer Name"
            value={quickName}
            onChange={(e) => setQuickName(e.target.value)}
          />
          <TextField
            fullWidth
            margin="dense"
            label="Phone Number"
            placeholder="e.g. 98XXXXXXXX"
            value={quickPhone}
            onChange={(e) => setQuickPhone(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setQuickCustomerOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!quickName.trim() || quickAddCustomer.isPending}
            onClick={() => quickAddCustomer.mutate()}
          >
            Add & Select
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}