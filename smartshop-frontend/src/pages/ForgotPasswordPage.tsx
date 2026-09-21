import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Box, Button, Card, TextField, Typography, Alert, CircularProgress } from '@mui/material';
import LockResetIcon from '@mui/icons-material/LockReset';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { extractErrorMessage } from '../lib/api';
import type { ApiResponse, ForgotPasswordRequest } from '../types';

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
});

type FormValues = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const [loading, setLoading] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');
    try {
      const payload: ForgotPasswordRequest = { email: values.email.trim() };
      await api.post<ApiResponse<void>>('/auth/forgot-password', payload);
      setSuccessMsg(
        'If an account exists with this email, a password reset token has been sent to your inbox.'
      );
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
          <LockResetIcon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h5" fontWeight="bold">Forgot Password</Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Enter your registered email address to receive a secure password reset token.
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
              to="/reset-password"
              sx={{ mb: 2 }}
            >
              Enter Reset Token
            </Button>
            <Button
              fullWidth
              variant="outlined"
              component={Link}
              to="/login"
              startIcon={<ArrowBackIcon />}
            >
              Back to Sign In
            </Button>
          </Box>
        ) : (
          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            <TextField
              label="Email Address"
              type="email"
              fullWidth
              margin="normal"
              {...register('email')}
              error={Boolean(errors.email)}
              helperText={errors.email?.message}
              placeholder="e.g. user@example.com"
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              size="large"
              sx={{ mt: 3, mb: 2 }}
              disabled={loading}
            >
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Send Reset Link'}
            </Button>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
              <Link to="/login" style={{ color: '#2D6A4F', fontSize: '0.875rem', textDecoration: 'none' }}>
                Back to Sign In
              </Link>
              <Link to="/reset-password" style={{ color: '#2D6A4F', fontSize: '0.875rem', textDecoration: 'none' }}>
                Already have a token?
              </Link>
            </Box>
          </Box>
        )}
      </Card>
    </Box>
  );
}
