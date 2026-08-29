import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Chip, CircularProgress, Alert, Button, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Pagination,
} from '@mui/material';
import { useState } from 'react';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse, PageResponse, ShopRegistration } from '../types';

type Filter = 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED';

const FILTERS: Filter[] = ['ALL', 'PENDING', 'APPROVED', 'REJECTED'];

function statusColor(status: string): 'success' | 'error' | 'warning' | 'default' {
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'error';
  if (status === 'PENDING') return 'warning';
  return 'default';
}

export default function AdminShopApprovalsPage() {
  const hasRole = useAuthStore((s) => s.hasRole);
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<Filter>('ALL');
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [approveTarget, setApproveTarget] = useState<ShopRegistration | null>(null);
  const [rejectTarget, setRejectTarget] = useState<ShopRegistration | null>(null);
  const [detailsTarget, setDetailsTarget] = useState<ShopRegistration | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['shop-registrations', filter, page],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PageResponse<ShopRegistration>>>('/shop-registrations', {
        params: {
          page,
          size: 10,
          ...(filter !== 'ALL' ? { status: filter } : {}),
        },
      });
      return res.data.data;
    },
    enabled: hasRole('SUPER_ADMIN'),
  });

  const approve = useMutation({
    mutationFn: async (id: string) => api.post(`/shop-registrations/${id}/approve`),
    onSuccess: () => {
      setApproveTarget(null);
      setSuccess('Application approved. The shop and owner invite code have been created.');
      queryClient.invalidateQueries({ queryKey: ['shop-registrations'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const reject = useMutation({
    mutationFn: async ({ id, rejectionReason }: { id: string; rejectionReason: string }) =>
      api.post(`/shop-registrations/${id}/reject`, { rejectionReason }),
    onSuccess: () => {
      setRejectTarget(null);
      setRejectReason('');
      setSuccess('Application rejected. The applicant has been notified via email.');
      queryClient.invalidateQueries({ queryKey: ['shop-registrations'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  if (!hasRole('SUPER_ADMIN')) {
    return <Alert severity="error">Access Denied. This page is for Super Admins only.</Alert>;
  }

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Shop Registration Requests
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
          {success}
        </Alert>
      )}

      <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
        {FILTERS.map((f) => (
          <Chip
            key={f}
            label={f === 'ALL' ? 'All' : f}
            clickable
            color={filter === f ? 'primary' : 'default'}
            variant={filter === f ? 'filled' : 'outlined'}
            onClick={() => {
              setFilter(f);
              setPage(0);
            }}
          />
        ))}
      </Box>

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Applicant Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Shop Name</TableCell>
                <TableCell>PAN/VAT</TableCell>
                <TableCell>Applied Date</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((r) => (
                <TableRow key={r.id} hover onClick={() => setDetailsTarget(r)} sx={{ cursor: 'pointer' }}>
                  <TableCell>{r.applicantName}</TableCell>
                  <TableCell>{r.applicantEmail}</TableCell>
                  <TableCell>{r.shopName}</TableCell>
                  <TableCell>{r.panVatNumber}</TableCell>
                  <TableCell>{new Date(r.createdAt).toLocaleDateString()}</TableCell>
                  <TableCell>
                    <Chip size="small" color={statusColor(r.status)} label={r.status} />
                    {r.status === 'REJECTED' && r.rejectionReason && (
                      <Typography variant="caption" display="block" color="text.secondary">
                        {r.rejectionReason}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {r.status === 'PENDING' ? (
                      <Box sx={{ display: 'flex', gap: 1 }}>
                        <Button
                          size="small"
                          variant="contained"
                          color="success"
                          onClick={(e) => { e.stopPropagation(); setApproveTarget(r); }}
                        >
                          Approve
                        </Button>
                        <Button
                          size="small"
                          variant="contained"
                          color="error"
                          onClick={(e) => { e.stopPropagation(); setRejectTarget(r); }}
                        >
                          Reject
                        </Button>
                      </Box>
                    ) : (
                      '-'
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>No applications found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={Boolean(approveTarget)} onClose={() => setApproveTarget(null)}>
        <DialogTitle>Approve Application</DialogTitle>
        <DialogContent>
          <Typography>
            Approve <strong>{approveTarget?.shopName}</strong>? This will create the shop and send
            the owner their invite code.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setApproveTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="success"
            disabled={approve.isPending}
            onClick={() => approveTarget && approve.mutate(approveTarget.id)}
          >
            {approve.isPending ? 'Approving...' : 'Approve'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(rejectTarget)} onClose={() => setRejectTarget(null)} fullWidth maxWidth="sm">
        <DialogTitle>Reject Application</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Reject application for <strong>{rejectTarget?.shopName}</strong>?
          </Typography>
          <TextField
            label="Rejection Reason"
            required
            fullWidth
            multiline
            rows={3}
            margin="dense"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            disabled={reject.isPending || !rejectReason.trim()}
            onClick={() =>
              rejectTarget && reject.mutate({ id: rejectTarget.id, rejectionReason: rejectReason.trim() })
            }
          >
            {reject.isPending ? 'Rejecting...' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog
        open={Boolean(detailsTarget)}
        onClose={() => setDetailsTarget(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Application Details</DialogTitle>
        <DialogContent>
          {detailsTarget && (
            <>
              <DetailRow label="Applicant" value={detailsTarget.applicantName} />
              <DetailRow label="Email" value={detailsTarget.applicantEmail} />
              <DetailRow label="Shop Name" value={detailsTarget.shopName} />
              <DetailRow label="PAN/VAT Number" value={detailsTarget.panVatNumber} />
              <DetailRow label="Phone" value={detailsTarget.phone ?? '-'} />
              <DetailRow label="Address" value={detailsTarget.address ?? '-'} />
              <DetailRow label="Applied Date" value={new Date(detailsTarget.createdAt).toLocaleString()} />
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                <Typography variant="body2" color="text.secondary" sx={{ minWidth: 140 }}>
                  Status:
                </Typography>
                <Chip size="small" color={statusColor(detailsTarget.status)} label={detailsTarget.status} />
              </Box>
              {detailsTarget.status === 'REJECTED' && detailsTarget.rejectionReason && (
                <Alert severity="warning" sx={{ mt: 2 }}>
                  Re-verification reason: {detailsTarget.rejectionReason}
                </Alert>
              )}
              {detailsTarget.panCertificateUrl ? (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    PAN Certificate
                  </Typography>
                  <Box
                    component="img"
                    src={detailsTarget.panCertificateUrl}
                    alt="PAN certificate"
                    sx={{
                      width: '100%',
                      maxHeight: 320,
                      objectFit: 'contain',
                      border: '1px solid',
                      borderColor: 'divider',
                      borderRadius: 1,
                      bgcolor: 'grey.50',
                    }}
                  />
                  <Button
                    size="small"
                    sx={{ mt: 1 }}
                    href={detailsTarget.panCertificateUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Open full image in new tab
                  </Button>
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  No PAN certificate was uploaded with this application.
                </Typography>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailsTarget(null)}>Close</Button>
          {detailsTarget?.status === 'PENDING' && (
            <>
              <Button
                variant="contained"
                color="success"
                onClick={() => {
                  const target = detailsTarget;
                  setDetailsTarget(null);
                  setApproveTarget(target);
                }}
              >
                Approve
              </Button>
              <Button
                variant="contained"
                color="error"
                onClick={() => {
                  const target = detailsTarget;
                  setDetailsTarget(null);
                  setRejectTarget(target);
                }}
              >
                Send for Re-verification
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ display: 'flex', gap: 1, mb: 0.5 }}>
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 140 }}>
        {label}:
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Box>
  );
}
