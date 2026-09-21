import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert, Chip, MenuItem, Divider,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import HistoryIcon from '@mui/icons-material/History';
import api, { extractErrorMessage } from '../lib/api';
import { AUDIT_ACTIONS, AUDIT_ENTITIES } from '../types';
import type { AuditLog, PageResponse } from '../types';

const PAGE_SIZE = 20;

interface Filters {
  userId: string;
  action: string;
  entityName: string;
  /** Plain yyyy-MM-dd from the date inputs — widened to ISO_DATE_TIME before it is sent. */
  dateFrom: string;
  dateTo: string;
}

const EMPTY_FILTERS: Filters = {
  userId: '',
  action: '',
  entityName: '',
  dateFrom: '',
  dateTo: '',
};

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

type ChipColor = 'default' | 'error' | 'info' | 'success' | 'warning';

/** `action` is a free-form string on the wire, so anything unrecognised falls through. */
function actionColor(action: string): ChipColor {
  switch (action) {
    case 'CREATE':
    case 'APPROVE':
      return 'success';
    case 'DELETE':
    case 'REJECT':
      return 'error';
    case 'UPDATE':
      return 'info';
    case 'ROLE_ASSIGN':
    case 'ROLE_REVOKE':
      return 'warning';
    default:
      return 'default';
  }
}

function formatWhen(iso: string): string {
  const d = new Date(iso);
  // LocalDateTime arrives without an offset; fall back to the raw string if unparseable.
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString();
}

interface HistoryTarget {
  entityName: string;
  entityId: string;
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" display="block">
        {label}
      </Typography>
      <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
        {value}
      </Typography>
    </Box>
  );
}

/**
 * `oldValue` / `newValue` are FREE-FORM TEXT written by the services — they are not
 * guaranteed to be JSON, so they are never parsed, only printed verbatim.
 */
function ValuePanel({ title, value }: { title: string; value?: string }) {
  return (
    <Card variant="outlined" sx={{ p: 1.5 }}>
      <Typography variant="subtitle2" gutterBottom>
        {title}
      </Typography>
      {value ? (
        <Typography
          component="pre"
          variant="caption"
          sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0 }}
        >
          {value}
        </Typography>
      ) : (
        <Typography variant="caption" color="text.secondary">
          Not recorded for this action
        </Typography>
      )}
    </Card>
  );
}

export default function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<Filters>(EMPTY_FILTERS);
  const [selected, setSelected] = useState<AuditLog | null>(null);
  const [historyTarget, setHistoryTarget] = useState<HistoryTarget | null>(null);

  // Any filter change must go back to the first page, otherwise the server can
  // legitimately answer with an empty page that looks like "no data".
  const patchFilters = (next: Partial<Filters>) => {
    setFilters((f) => ({ ...f, ...next }));
    setPage(0);
  };

  const filtersActive = Object.values(filters).some(Boolean);
  const trimmedUserId = filters.userId.trim();
  // The server binds `userId` straight to a UUID, so a partial string is a 400.
  // Only forward it once it is a well-formed UUID and say so in the helper text.
  const userIdValid = trimmedUserId.length === 0 || UUID_RE.test(trimmedUserId);
  const userIdParam = userIdValid ? trimmedUserId : '';

  const { data, isLoading, isError, error: queryError } = useQuery({
    queryKey: [
      'audit-logs',
      page,
      userIdParam,
      filters.action,
      filters.entityName,
      filters.dateFrom,
      filters.dateTo,
    ],
    queryFn: async () => {
      const params: Record<string, string | number> = { page, size: PAGE_SIZE };
      // `action` and `entityName` are matched with cb.equal — exact and CASE-SENSITIVE —
      // which is why both are dropdowns off the backend constant lists. Blank is ignored
      // server-side, but an empty string would still travel, so omit it here.
      if (userIdParam) params.userId = userIdParam;
      if (filters.action) params.action = filters.action;
      if (filters.entityName) params.entityName = filters.entityName;
      // TRAP: unlike every other date filter in this app, /audit-logs binds dateFrom and
      // dateTo as LocalDateTime via ISO_DATE_TIME. A bare 'yyyy-MM-dd' is a 400, and so is
      // a trailing 'Z'. The time component must always be appended; dateTo uses end-of-day
      // so the selected day is inclusive. Do not "simplify" this back to the raw input.
      if (filters.dateFrom) params.dateFrom = `${filters.dateFrom}T00:00:00`;
      if (filters.dateTo) params.dateTo = `${filters.dateTo}T23:59:59`;
      // There is no `sort` param on this endpoint — ordering is fixed at createdAt DESC.
      const res = await api.get<{ data: PageResponse<AuditLog> }>('/audit-logs', { params });
      return res.data.data;
    },
    // A 403 from the role check is not worth retrying three times.
    retry: false,
  });

  /**
   * Per-entity history. UNPAGED and UNCAPPED server-side: a hot entity can come back with
   * hundreds of rows in one array, so the dialog scrolls rather than paginates.
   */
  const {
    data: history,
    isLoading: historyLoading,
    isError: historyIsError,
    error: historyError,
  } = useQuery({
    queryKey: ['audit-logs', 'entity', historyTarget?.entityName, historyTarget?.entityId],
    queryFn: async () => {
      const res = await api.get<{ data: AuditLog[] }>(
        `/audit-logs/entity/${encodeURIComponent(historyTarget!.entityName)}/${encodeURIComponent(
          historyTarget!.entityId,
        )}`,
      );
      return res.data.data;
    },
    enabled: Boolean(historyTarget),
    retry: false,
  });

  const rows = data?.content ?? [];
  const historyRows = history ?? [];

  const emptyMessage = isError
    ? 'Audit records could not be loaded — see the message above.'
    : filtersActive
      ? 'No audit records match these filters. Action and entity are matched exactly, and the user filter needs a full UUID — try widening the date range or clearing the filters.'
      : 'No audit records yet. Rows are written automatically when shops, branches, products, purchases, sales and role assignments change.';

  const closeDetail = () => {
    setSelected(null);
    setHistoryTarget(null);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5">Audit Logs</Typography>
        <Typography variant="body2" color="text.secondary">
          Read-only trail
        </Typography>
      </Box>

      {/* The controller applies no shop or branch predicate, so this cannot be narrowed
          server-side. Surfaced permanently so nobody reads other shops' rows as a bug. */}
      <Alert severity="info" sx={{ mb: 2 }}>
        Audit records are platform-wide. This view is not scoped to your shop, and rows for other
        shops cannot be filtered out server-side.
      </Alert>

      {isError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {extractErrorMessage(queryError)} The audit trail is restricted to platform super-admins
          and shop admins.
        </Alert>
      )}

      <Card sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <TextField
            size="small"
            label="User ID (UUID)"
            sx={{ minWidth: 300 }}
            value={filters.userId}
            error={!userIdValid}
            helperText={
              userIdValid
                ? 'Exact match — paste a full user UUID'
                : 'Not a complete UUID yet, so this filter is not being applied'
            }
            onChange={(e) => patchFilters({ userId: e.target.value })}
          />

          {/* Exact, case-sensitive server-side match — free text here would be a trap. */}
          <TextField
            select
            size="small"
            label="Action"
            sx={{ minWidth: 180 }}
            value={filters.action}
            onChange={(e) => patchFilters({ action: e.target.value })}
          >
            <MenuItem value="">All actions</MenuItem>
            {AUDIT_ACTIONS.map((a) => (
              <MenuItem key={a} value={a}>
                {a.replace(/_/g, ' ')}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            select
            size="small"
            label="Entity"
            sx={{ minWidth: 200 }}
            value={filters.entityName}
            onChange={(e) => patchFilters({ entityName: e.target.value })}
          >
            <MenuItem value="">All entities</MenuItem>
            {AUDIT_ENTITIES.map((e) => (
              <MenuItem key={e} value={e}>
                {e}
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
            helperText="Inclusive — sent as 23:59:59"
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
                <TableCell>User</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>IP</TableCell>
                <TableCell align="right"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((log) => (
                <TableRow key={log.id}>
                  <TableCell>{formatWhen(log.createdAt)}</TableCell>
                  {/* userId and userName are absent together on system-generated rows. */}
                  <TableCell>{log.userName ?? 'System'}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      variant="outlined"
                      color={actionColor(log.action)}
                      label={log.action.replace(/_/g, ' ')}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{log.entityName ?? '-'}</Typography>
                    {log.entityId && (
                      <Typography variant="caption" color="text.secondary">
                        {log.entityId.slice(0, 8)}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>{log.ipAddress ?? '-'}</TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      aria-label="View audit record"
                      onClick={() => setSelected(log)}
                    >
                      <VisibilityIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>{emptyMessage}</TableCell>
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
          {data?.totalElements ?? 0} records
        </Typography>
      </Box>

      <Dialog open={Boolean(selected)} onClose={closeDetail} fullWidth maxWidth="md">
        <DialogTitle>Audit Record</DialogTitle>
        <DialogContent>
          {selected && (
            <Box>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' },
                  gap: 2,
                  mb: 2,
                }}
              >
                <Field label="When" value={formatWhen(selected.createdAt)} />
                <Field label="Action" value={selected.action} />
                <Field label="IP address" value={selected.ipAddress ?? '-'} />
                <Field label="User" value={selected.userName ?? 'System'} />
                <Field label="User ID" value={selected.userId ?? '-'} />
                <Field label="Record ID" value={selected.id} />
                <Field label="Entity" value={selected.entityName ?? '-'} />
                <Field label="Entity ID" value={selected.entityId ?? '-'} />
                <Field label="Raw timestamp" value={selected.createdAt} />
              </Box>
              <Divider sx={{ mb: 2 }} />
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                  gap: 2,
                }}
              >
                <ValuePanel title="Before" value={selected.oldValue} />
                <ValuePanel title="After" value={selected.newValue} />
              </Box>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                Before / after are free-form text written by the service that made the change, not
                structured JSON.
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          {selected?.entityName && selected?.entityId && (
            <Button
              startIcon={<HistoryIcon />}
              onClick={() =>
                setHistoryTarget({
                  entityName: selected.entityName as string,
                  entityId: selected.entityId as string,
                })
              }
            >
              View full history for this record
            </Button>
          )}
          <Button onClick={closeDetail}>Close</Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(historyTarget)}
        onClose={() => setHistoryTarget(null)}
        fullWidth
        maxWidth="md"
      >
        <DialogTitle>
          {historyTarget
            ? `History — ${historyTarget.entityName} ${historyTarget.entityId.slice(0, 8)}`
            : 'History'}
        </DialogTitle>
        <DialogContent>
          {historyIsError && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              {extractErrorMessage(historyError)}
            </Alert>
          )}
          {historyLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
                {historyRows.length} record{historyRows.length === 1 ? '' : 's'} — this endpoint is
                unpaged, so every entry for this record is listed below.
              </Typography>
              {/* Uncapped response: scroll instead of trying to paginate client-side. */}
              <Box sx={{ maxHeight: 420, overflow: 'auto' }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>When</TableCell>
                      <TableCell>User</TableCell>
                      <TableCell>Action</TableCell>
                      <TableCell>Change</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {historyRows.map((log) => (
                      <TableRow key={log.id}>
                        <TableCell sx={{ whiteSpace: 'nowrap' }}>
                          {formatWhen(log.createdAt)}
                        </TableCell>
                        <TableCell>{log.userName ?? 'System'}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            variant="outlined"
                            color={actionColor(log.action)}
                            label={log.action.replace(/_/g, ' ')}
                          />
                        </TableCell>
                        <TableCell>
                          {!log.oldValue && !log.newValue ? (
                            <Typography variant="caption" color="text.secondary">
                              No before/after recorded
                            </Typography>
                          ) : (
                            <Box>
                              {log.oldValue && (
                                <Typography
                                  component="pre"
                                  variant="caption"
                                  sx={{
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-word',
                                    m: 0,
                                    color: 'text.secondary',
                                  }}
                                >
                                  {`- ${log.oldValue}`}
                                </Typography>
                              )}
                              {log.newValue && (
                                <Typography
                                  component="pre"
                                  variant="caption"
                                  sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', m: 0 }}
                                >
                                  {`+ ${log.newValue}`}
                                </Typography>
                              )}
                            </Box>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                    {historyRows.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4}>
                          No audit records for this entity — the trail only covers changes made
                          after auditing was enabled.
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setHistoryTarget(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
