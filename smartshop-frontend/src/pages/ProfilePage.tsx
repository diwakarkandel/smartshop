import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, TextField, Button, Alert, CircularProgress,
  Avatar, Divider, Chip,
} from '@mui/material';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import ImageUpload from '../components/common/ImageUpload';
import type { ApiResponse, User } from '../types';

export default function ProfilePage() {
  const authUser = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [form, setForm] = useState({ firstName: '', lastName: '', phone: '' });
  const [profileImageUrl, setProfileImageUrl] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  const { data: profile, isLoading } = useQuery<User>({
    queryKey: ['me'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<User>>('/users/me');
      return res.data.data;
    },
  });

  useEffect(() => {
    if (profile && !editing) {
      const [first, ...rest] = (profile.fullName ?? '').split(' ');
      setForm({ firstName: first ?? '', lastName: rest.join(' '), phone: profile.phone ?? '' });
      setProfileImageUrl(profile.profileImageUrl ?? null);
    }
  }, [profile, editing]);

  const save = useMutation({
    mutationFn: async () =>
      api.patch('/users/me', {
        firstName: form.firstName || undefined,
        lastName: form.lastName || undefined,
        phone: form.phone,
        profileImageUrl: profileImageUrl,
      }),
    onSuccess: () => {
      setMessage('Profile updated successfully!');
      setError('');
      setEditing(false);
      queryClient.invalidateQueries({ queryKey: ['me'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto' }}>
      <Typography variant="h5" sx={{ mb: 3 }}>My Profile</Typography>

      {message && <Alert severity="success" sx={{ mb: 2 }} onClose={() => setMessage('')}>{message}</Alert>}
      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}
      {profile && !profile.emailVerified && (
        <Alert
          severity="warning"
          sx={{ mb: 2 }}
          action={
            <Button color="inherit" size="small" component={Link} to="/verify-email">
              Verify Now
            </Button>
          }
        >
          Your email address is not yet verified.
        </Alert>
      )}

      <Card sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 3 }}>
          <Avatar
            src={profileImageUrl ?? profile?.profileImageUrl}
            sx={{ width: 80, height: 80, fontSize: 32 }}
          >
            {profile?.fullName?.[0]?.toUpperCase()}
          </Avatar>
          <Box>
            <Typography variant="h6">{profile?.fullName}</Typography>
            <Typography variant="body2" color="text.secondary">{profile?.email}</Typography>
            <Box sx={{ mt: 0.5 }}>
              {authUser?.roles?.map((r) => (
                <Chip key={r} label={r} size="small" sx={{ mr: 0.5 }} />
              ))}
            </Box>
          </Box>
        </Box>

        <Divider sx={{ mb: 3 }} />

        {editing ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <ImageUpload
              value={profileImageUrl}
              onChange={setProfileImageUrl}
              label="Profile Picture"
              size={80}
            />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                label="First Name"
                fullWidth
                size="small"
                value={form.firstName}
                onChange={(e) => setForm({ ...form, firstName: e.target.value })}
              />
              <TextField
                label="Last Name"
                fullWidth
                size="small"
                value={form.lastName}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
              />
            </Box>
            <TextField
              label="Phone"
              fullWidth
              size="small"
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
            <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
              <Button onClick={() => setEditing(false)}>Cancel</Button>
              <Button variant="contained" disabled={save.isPending} onClick={() => save.mutate()}>
                Save Changes
              </Button>
            </Box>
          </Box>
        ) : (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5, alignItems: 'center' }}>
              <Typography variant="body2" color="text.secondary">Email</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="body2">{profile?.email}</Typography>
                {profile?.emailVerified ? (
                  <Chip size="small" label="Verified" color="success" />
                ) : (
                  <Chip
                    size="small"
                    label="Unverified"
                    color="warning"
                    component={Link}
                    to="/verify-email"
                    clickable
                  />
                )}
              </Box>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography variant="body2" color="text.secondary">Phone</Typography>
              <Typography variant="body2">{profile?.phone || '-'}</Typography>
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1.5 }}>
              <Typography variant="body2" color="text.secondary">Status</Typography>
              <Chip size="small" color={profile?.status === 'ACTIVE' ? 'success' : 'warning'} label={profile?.status} />
            </Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="body2" color="text.secondary">Member since</Typography>
              <Typography variant="body2">{profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '-'}</Typography>
            </Box>
            <Divider sx={{ mb: 2 }} />
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>Branch Roles</Typography>
            {(profile?.branchRoles ?? []).length === 0 ? (
              <Typography variant="body2" color="text.secondary">No branch roles assigned.</Typography>
            ) : (
              (profile?.branchRoles ?? []).map((br, i) => (
                <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2">{br.branchName ?? 'Shop-wide'}</Typography>
                  <Chip size="small" label={br.role} />
                </Box>
              ))
            )}
            <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
              <Button variant="outlined" onClick={() => setEditing(true)}>Edit Profile</Button>
            </Box>
          </Box>
        )}
      </Card>
    </Box>
  );
}
