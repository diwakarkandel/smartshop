import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import {
  Box, Card, Typography, TextField, Button, Alert, CircularProgress, Divider,
} from '@mui/material';
import { useState, useRef } from 'react';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse, UserProfile, ShopRegistration } from '../types';

interface UploadedFile {
  url: string;
  fileName: string;
}

export default function ShopRegistrationPage() {
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [reapply, setReapply] = useState(false);
  const [form, setForm] = useState({ shopName: '', panVatNumber: '', phone: '', address: '' });
  const [certificate, setCertificate] = useState<UploadedFile | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isAdmin = user?.roles.includes('SUPER_ADMIN') || user?.roles.includes('SHOP_ADMIN');

  const { data: profile } = useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<UserProfile>>('/users/me');
      return res.data.data;
    },
  });

  const { data: application, isLoading } = useQuery({
    queryKey: ['my-application'],
    queryFn: async (): Promise<ShopRegistration | null> => {
      try {
        const res = await api.get<ApiResponse<ShopRegistration>>('/shop-registrations/my-application');
        return res.data.data;
      } catch (err) {
        if (isAxiosError(err) && err.response?.status === 404) return null;
        throw err;
      }
    },
  });

  const uploadCertificate = useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post<{ url: string }>('/test/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return res.data;
    },
    onSuccess: (data, file) => {
      setError('');
      setCertificate({ url: data.url, fileName: file.name });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const apply = useMutation({
    mutationFn: async () =>
      api.post('/shop-registrations/apply', {
        shopName: form.shopName.trim(),
        panVatNumber: form.panVatNumber.trim(),
        panCertificateUrl: certificate?.url,
        phone: form.phone.trim() || undefined,
        address: form.address.trim() || undefined,
      }),
    onSuccess: () => {
      setForm({ shopName: '', panVatNumber: '', phone: '', address: '' });
      setCertificate(null);
      setReapply(false);
      setError('');
      queryClient.invalidateQueries({ queryKey: ['my-application'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) uploadCertificate.mutate(file);
    event.target.value = '';
  };

  if (isAdmin) {
    return (
      <Alert severity="info">
        You already have shop administration access, so you don&apos;t need to apply for a shop.
      </Alert>
    );
  }

  const showForm = reapply || !application;

  return (
    <Box sx={{ maxWidth: 640, mx: 'auto' }}>
      <Typography variant="h5" gutterBottom>
        Apply to Become a Shop Owner
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : !showForm && application ? (
        <Card sx={{ p: 3 }}>
          {application.status === 'PENDING' && (
            <>
              <Alert severity="warning" sx={{ mb: 2 }}>
                Application Submitted! Your application is under review. We&apos;ll notify you via
                email once a decision is made.
              </Alert>
              <DetailRow label="Shop Name" value={application.shopName} />
              <DetailRow label="PAN/VAT Number" value={application.panVatNumber} />
            </>
          )}
          {application.status === 'APPROVED' && (
            <Alert severity="success">
              Congratulations! Your shop &quot;{application.shopName}&quot; has been approved. Log
              out and back in to access your shop admin features.
            </Alert>
          )}
          {application.status === 'REJECTED' && (
            <>
              <Alert severity="error" sx={{ mb: 2 }}>
                Your application for &quot;{application.shopName}&quot; was rejected.
                {application.rejectionReason ? ` Reason: ${application.rejectionReason}` : ''}
              </Alert>
              <Button variant="contained" onClick={() => setReapply(true)}>
                Reapply
              </Button>
            </>
          )}
        </Card>
      ) : (
        <Card sx={{ p: 3 }}>
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            Applicant Details
          </Typography>
          <DetailRow label="Name" value={profile?.fullName ?? ''} />
          <DetailRow label="Email" value={profile?.email ?? user?.email ?? ''} />
          <Divider sx={{ my: 2 }} />
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            Proposed Shop Details
          </Typography>
          <TextField
            label="Shop Name"
            required
            fullWidth
            margin="dense"
            value={form.shopName}
            onChange={(e) => setForm({ ...form, shopName: e.target.value })}
          />
          <TextField
            label="PAN/VAT Number"
            required
            fullWidth
            margin="dense"
            value={form.panVatNumber}
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
            label="Address"
            fullWidth
            multiline
            rows={3}
            margin="dense"
            value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
          />

          <Typography variant="subtitle2" color="text.secondary" sx={{ mt: 2, mb: 1 }}>
            PAN Certificate (required)
          </Typography>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            hidden
            onChange={handleFileSelect}
          />
          {certificate ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box
                component="img"
                src={certificate.url}
                alt="PAN certificate"
                sx={{ width: 96, height: 72, objectFit: 'cover', borderRadius: 1, border: '1px solid', borderColor: 'divider' }}
              />
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body2" noWrap>
                  {certificate.fileName}
                </Typography>
                <Button size="small" onClick={() => fileInputRef.current?.click()} disabled={uploadCertificate.isPending}>
                  Replace
                </Button>
                <Button size="small" color="error" onClick={() => setCertificate(null)}>
                  Remove
                </Button>
              </Box>
              {uploadCertificate.isPending && <CircularProgress size={20} />}
            </Box>
          ) : (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Button variant="outlined" onClick={() => fileInputRef.current?.click()} disabled={uploadCertificate.isPending}>
                {uploadCertificate.isPending ? 'Uploading...' : 'Upload PAN Certificate'}
              </Button>
              {uploadCertificate.isPending && <CircularProgress size={20} />}
            </Box>
          )}

          <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            {reapply && (
              <Button onClick={() => setReapply(false)}>Cancel</Button>
            )}
            <Button
              variant="contained"
              disabled={
                apply.isPending ||
                uploadCertificate.isPending ||
                !form.shopName.trim() ||
                !form.panVatNumber.trim() ||
                !certificate
              }
              onClick={() => apply.mutate()}
            >
              {apply.isPending ? 'Submitting...' : 'Submit Application'}
            </Button>
          </Box>
        </Card>
      )}
    </Box>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ display: 'flex', gap: 1, mb: 0.5 }}>
      <Typography variant="body2" color="text.secondary" sx={{ minWidth: 140 }}>
        {label}:
      </Typography>
      <Typography variant="body2">{value || '-'}</Typography>
    </Box>
  );
}
