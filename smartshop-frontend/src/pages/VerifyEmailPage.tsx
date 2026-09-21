import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Box, Button, Card, TextField, Typography, Alert, CircularProgress, Divider } from '@mui/material';
import MarkEmailReadIcon from '@mui/icons-material/MarkEmailRead';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import api, { extractErrorMessage } from '../lib/api';
import type { ApiResponse, VerifyEmailRequest } from '../types';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const tokenParam = searchParams.get('token') || '';

  const [token, setToken] = useState(tokenParam);
  const [resendEmail, setResendEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [resendSuccess, setResendSuccess] = useState('');

  const handleVerify = async (tokenToVerify: string) => {
    if (!tokenToVerify.trim()) {
      setErrorMsg('Please enter your verification token');
      return;
    }
    setLoading(true);
    setErrorMsg('');
    setSuccessMsg('');
    try {
      const payload: VerifyEmailRequest = { token: tokenToVerify.trim() };
      await api.post<ApiResponse<void>>('/auth/verify-email', payload);
      setSuccessMsg('Your email address has been successfully verified! You have full access to SmartShop.');
    } catch (err) {
      setErrorMsg(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (tokenParam) {
      void handleVerify(tokenParam);
    }
  }, [tokenParam]);

  const handleResend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resendEmail.trim()) return;
    setResending(true);
    setResendSuccess('');
    try {
      await api.post<ApiResponse<void>>('/auth/resend-verification', null, {
        params: { email: resendEmail.trim() },
      });
      setResendSuccess('A new verification email has been dispatched. Check your inbox.');
    } catch (err) {
      setErrorMsg(extractErrorMessage(err));
    } finally {
      setResending(false);
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
      <Card sx={{ width: 460, p: 4, borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
          <MarkEmailReadIcon color="primary" sx={{ fontSize: 32 }} />
          <Typography variant="h5" fontWeight="bold">Verify Email</Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Verify your email address to ensure full account access and security.
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
              Continue to Sign In
            </Button>
          </Box>
        ) : (
          <Box>
            <TextField
              label="Verification Token"
              fullWidth
              margin="normal"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="Paste verification token here"
            />
            <Button
              fullWidth
              variant="contained"
              size="large"
              sx={{ mt: 2, mb: 3 }}
              disabled={loading}
              onClick={() => handleVerify(token)}
            >
              {loading ? <CircularProgress size={22} color="inherit" /> : 'Verify Account'}
            </Button>

            <Divider sx={{ my: 2 }}>
              <Typography variant="caption" color="text.secondary">
                DIDN'T RECEIVE A CODE?
              </Typography>
            </Divider>

            {resendSuccess && (
              <Alert severity="info" sx={{ mb: 2 }}>
                {resendSuccess}
              </Alert>
            )}

            <Box component="form" onSubmit={handleResend}>
              <TextField
                label="Registered Email"
                type="email"
                size="small"
                fullWidth
                margin="dense"
                value={resendEmail}
                onChange={(e) => setResendEmail(e.target.value)}
                placeholder="user@example.com"
              />
              <Button
                type="submit"
                fullWidth
                variant="outlined"
                size="small"
                sx={{ mt: 1 }}
                disabled={resending || !resendEmail.trim()}
              >
                {resending ? <CircularProgress size={18} /> : 'Resend Verification Code'}
              </Button>
            </Box>

            <Box sx={{ textAlign: 'center', mt: 3 }}>
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
