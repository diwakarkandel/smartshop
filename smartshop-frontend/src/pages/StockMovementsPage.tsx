import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, TextField, Pagination, CircularProgress, Alert, Chip, MenuItem,
} from '@mui/material';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId } from '../stores/shopStore';
import { MOVEMENT_TYPES } from '../types';
import type { Branch, MovementType, PageResponse, Product, StockMovement } from '../types';

interface Filters {
  branchId: string;
  productId: string;
  movementType: MovementType | '';
  dateFrom: string;
  dateTo: string;
}

const EMPTY_FILTERS: Filters = {
  branchId: '',
  productId: '',
  movementType: '',
  dateFrom: '',
  dateTo: '',
};

/**
 * `quantity` is ALWAYS POSITIVE on the wire — the ledger stores magnitude only, so
 * the direction of a row has to be derived from its movement type. Everything that
 * ends in `_IN`, plus `OPENING_STOCK`, adds stock; every `_OUT` removes it.
 */
function isInbound(t: MovementType): boolean {
  return t === 'OPENING_STOCK' || t.endsWith('_IN');
}

function formatWhen(iso: string): string {
  const d = new Date(iso);
  // LocalDateTime arrives without an offset; fall back to the raw string if unparseable.
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
}

export default function StockMovementsPage() {
  const shopId = useDefaultShopId();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);

  // Any filter change must go back to the first page, otherwise the server can
  // legitimately answer with an empty page that looks like "no data".
  const patchFilters = (next: Partial<Filters>) => {
    setFilters((f) => ({ ...f, ...next }));
    setPage(0);
  };

  const filtersActive = Object.values(filters).some(Boolean);

  // Branch options come from the shop's real branches (not the user's grants), so a
  // shop-admin whose grant is shop-level still sees every branch to filter by.
  const { data: branchData } = useQuery({
    queryKey: ['branches', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', { params: { shopId } });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });
  const branches = (branchData ?? []).map((b) => ({ id: b.id, name: b.name }));

  // First 100 products only — there is no lightweight lookup endpoint for this filter.
  const { data: products } = useQuery({
    queryKey: ['products-simple', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Product> }>('/products', {
        params: { shopId, page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const { data, isLoading, isError, error: queryError } = useQuery({
    queryKey: [
      'stock-movements',
      shopId,
      page,
      filters.branchId,
      filters.productId,
      filters.movementType,
      filters.dateFrom,
      filters.dateTo,
    ],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<StockMovement> }>('/stock-movements', {
        // Empty controls must be omitted entirely (axios drops `undefined`): an empty
        // string reaches the server as a real value and `movementType` is enum-parsed,
        // so `''` would 400. There is no `sort` param — ordering is createdAt DESC.
        params: {
          shopId,
          page,
          size: 10,
          branchId: filters.branchId || undefined,
          productId: filters.productId || undefined,
          movementType: filters.movementType || undefined,
          // LocalDate bounds: plain yyyy-MM-dd, both inclusive whole days.
          dateFrom: filters.dateFrom || undefined,
          dateTo: filters.dateTo || undefined,
        },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
    // A 403 from the role check is not worth retrying three times.
    retry: false,
  });

  const rows = data?.content ?? [];

  const emptyMessage = isError
    ? 'Stock movements could not be loaded — see the message above.'
    : filtersActive
      ? 'No stock movements match these filters. Try widening the date range, or clear the filters.'
      : 'No stock movements recorded yet. Rows appear automatically when purchases, sales, returns and transfers are posted.';

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Stock Movements</Typography>
        <Typography variant="body2" color="text.secondary">
          Read-only ledger
        </Typography>
      </Box>

      {!shopId && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Select a shop from the top bar to continue.
        </Alert>
      )}

      {isError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {extractErrorMessage(queryError)} The stock ledger is restricted to shop admins,
          managers, inventory staff and accountants; platform super-admins are not granted
          access to branch-level stock data.
        </Alert>
      )}

      <Card sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            select
            size="small"
            label="Branch"
            sx={{ minWidth: 180 }}
            value={filters.branchId}
            onChange={(e) => patchFilters({ branchId: e.target.value })}
          >
            <MenuItem value="">All branches</MenuItem>
            {branches.map((b) => (
              <MenuItem key={b.id} value={b.id}>
                {b.name}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            select
            size="small"
            label="Product"
            sx={{ minWidth: 220 }}
            value={filters.productId}
            onChange={(e) => patchFilters({ productId: e.target.value })}
          >
            <MenuItem value="">All products</MenuItem>
            {(products ?? []).map((p) => (
              <MenuItem key={p.id} value={p.id}>
                {p.name} ({p.sku})
              </MenuItem>
            ))}
          </TextField>

          {/* Nothing in the current backend writes ADJUSTMENT_IN, ADJUSTMENT_OUT or
              OPENING_STOCK yet, so those options legitimately return empty pages. */}
          <TextField
            select
            size="small"
            label="Type"
            sx={{ minWidth: 200 }}
            value={filters.movementType}
            onChange={(e) => patchFilters({ movementType: e.target.value as MovementType | '' })}
          >
            <MenuItem value="">All types</MenuItem>
            {MOVEMENT_TYPES.map((t) => (
              <MenuItem key={t} value={t}>
                {t.replace(/_/g, ' ')}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            type="date"
            size="small"
            label="From"
            slotProps={{ inputLabel: { shrink: true } }}
            value={filters.dateFrom}
            onChange={(e) => patchFilters({ dateFrom: e.target.value })}
          />
          <TextField
            type="date"
            size="small"
            label="To"
            slotProps={{ inputLabel: { shrink: true } }}
            value={filters.dateTo}
            onChange={(e) => patchFilters({ dateTo: e.target.value })}
          />

          <Button
            onClick={() => {
              setFilters(EMPTY_FILTERS);
              setPage(0);
            }}
            disabled={!filtersActive}
          >
            Clear filters
          </Button>
        </Box>
      </Card>

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>When</TableCell>
                <TableCell>Item</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Qty</TableCell>
                <TableCell>Reference</TableCell>
                <TableCell>By</TableCell>
                <TableCell>Note</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((m) => {
                const inbound = isInbound(m.movementType);
                return (
                  <TableRow key={m.id}>
                    <TableCell>{formatWhen(m.createdAt)}</TableCell>
                    <TableCell>
                      <Typography variant="body2">{m.productName}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {m.sku}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        variant="outlined"
                        color={inbound ? 'success' : 'error'}
                        label={m.movementType.replace(/_/g, ' ')}
                      />
                    </TableCell>
                    <TableCell
                      align="right"
                      sx={{ color: inbound ? 'success.main' : 'error.main', fontWeight: 600 }}
                    >
                      {inbound ? '+' : '-'}
                      {m.quantity}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{m.referenceType ?? '-'}</Typography>
                      {m.referenceId && (
                        <Typography variant="caption" color="text.secondary">
                          {m.referenceId.slice(0, 8)}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{m.createdByName ?? 'System'}</TableCell>
                    <TableCell>{m.note ?? '-'}</TableCell>
                  </TableRow>
                );
              })}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>{emptyMessage}</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 2 }}>
        <Pagination
          count={data?.totalPages ?? 1}
          page={page + 1}
          onChange={(_, p) => setPage(p - 1)}
        />
        <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
          {data?.totalElements ?? 0} movements
        </Typography>
      </Box>
    </Box>
  );
}
