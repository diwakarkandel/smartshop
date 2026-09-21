import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Box, Button, Card, TextField, Typography, Alert, CircularProgress } from '@mui/material';
import StorefrontIcon from '@mui/icons-material/Storefront';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import { getDefaultRoute } from '../lib/routeRoles';
import type { AuthUser } from '../types';

const schema = z.object({
  email: z.string().email('Enter a valid email'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const onSubmit = async (values: FormValues) => {
    setError('');
    setLoading(true);
    try {
      const res = await api.post<{ data: AuthUser }>('/auth/login', values);
      setSession(res.data.data);
      navigate(getDefaultRoute(res.data.data.roles));
    } catch (err) {
      setError(extractErrorMessage(err));
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
        p: 2,
        position: 'relative',
        overflow: 'hidden',
        background: 'linear-gradient(135deg, #12362A 0%, #1B4332 45%, #2D6A4F 100%)',
        '&::before': {
          content: '""',
          position: 'absolute',
          inset: 0,
          background:
            'radial-gradient(600px 400px at 15% 20%, rgba(64,145,108,0.45), transparent 60%),' +
            'radial-gradient(500px 400px at 85% 80%, rgba(232,223,202,0.18), transparent 60%)',
          pointerEvents: 'none',
        },
      }}
    >
      <Card
        sx={{
          width: 400,
          maxWidth: '100%',
          p: 4,
          position: 'relative',
          borderRadius: 4,
          boxShadow: '0 30px 70px rgba(0,0,0,0.35)',
          animation: 'ss-pop 0.5s cubic-bezier(0.22,1,0.36,1) both',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, mb: 3 }}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 44,
              height: 44,
              borderRadius: 2.5,
              color: '#fff',
              background: 'linear-gradient(135deg, #1B4332 0%, #40916C 100%)',
              boxShadow: '0 6px 16px rgba(27,67,50,0.35)',
            }}
          >
            <StorefrontIcon />
          </Box>
          <Typography variant="h5" fontWeight={800}>SmartShop</Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Sign in to manage your shop
        </Typography>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <TextField
            label="Email"
            type="email"
            fullWidth
            margin="normal"
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
          />
          <TextField
            label="Password"
            type="password"
            fullWidth
            margin="normal"
            {...register('password')}
            error={Boolean(errors.password)}
            helperText={errors.password?.message}
          />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.5 }}>
            <Link to="/forgot-password" style={{ color: '#2D6A4F', fontSize: '0.85rem', textDecoration: 'none' }}>
              Forgot password?
            </Link>
          </Box>
          <Button
            type="submit"
            fullWidth
            variant="contained"
            size="large"
            sx={{ mt: 2.5 }}
            disabled={loading}
          >
            {loading ? <CircularProgress size={22} color="inherit" /> : 'Sign in'}
          </Button>
        </Box>
        <Typography variant="body2" sx={{ mt: 2, textAlign: 'center' }}>
          Don't have an account?{' '}
          <Link to="/register" style={{ color: '#2D6A4F' }}>
            Sign up
          </Link>
        </Typography>
      </Card>
    </Box>
  );
}