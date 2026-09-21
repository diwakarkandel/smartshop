import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Box, Button, Card, TextField, Typography, Alert, CircularProgress } from '@mui/material';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { extractErrorMessage } from '../lib/api';
import type { ApiResponse, ResetPasswordRequest } from '../types';

const schema = z
  .object({
    token: z.string().min(1, 'Reset token is required'),
    newPassword: z.string().min(6, 'Password must be at least 6 characters'),
    confirmPassword: z.string().min(6, 'Please confirm your password'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type FormValues = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const initialToken = searchParams.get('token') || '';

  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      token: initialToken,
      newPassword: '',
      confirmPassword: '',
    },
  });

  const onSubmit = async (values: FormValues) => {
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');
    try {
      const payload: ResetPasswordRequest = {
        token: values.token.trim(),
        newPassword: values.newPassword,
      };
      await api.post<ApiResponse<void>>('/auth/reset-password', payload);
      setSuccessMsg('Your password has been reset successfully! You can now sign in.');
    } catch (err) {
      setErrorMsg(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#1B4332',
        p: 2,
      }}
    >
      <Card sx={{ width: 440, p: 4, borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
          <LockOpenIcon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h5" fontWeight="bold">Reset Password</Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Enter the token sent to your email and your new password.
        </Typography>

        {errorMsg && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorMsg}
          </Alert>
        )}

        {successMsg ? (
          <Box>
            <Alert severity="success" sx={{ mb: 3 }}>
              {successMsg}
            </Alert>
            <Button
              fullWidth
              variant="contained"
              component={Link}
              to="/login"
              startIcon={<ArrowBackIcon />}
            >
              Go to Sign In
            </Button>
          </Box>
        ) : (
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="Reset Token"
              fullWidth
              margin="normal"
              {...register('token')}
              error={Boolean(errors.token)}
              helperText={errors.token?.message}
              placeholder="Paste token from email"
            />
            <TextField
              label="New Password"
              type="password"
              fullWidth
              margin="normal"
              {...register('newPassword')}
              error={Boolean(errors.newPassword)}
              helperText={errors.newPassword?.message}
            />
            <TextField
              label="Confirm New Password"
              type="password"
              fullWidth
              margin="normal"
              {...register('confirmPassword')}
              error={Boolean(errors.confirmPassword)}
              helperText={errors.confirmPassword?.message}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Set New Password'}
            </Button>
            <Box sx={{ textAlign: 'center', mt: 1 }}>
              <Link to="/login" style={{ color: '#2D6A4F', fontSize: '0.875rem', textDecoration: 'none' }}>
                Back to Sign In
              </Link>
            </Box>
          </Box>
        )}
      </Card>
    </Box>
  );
}
