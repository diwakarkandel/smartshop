import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, Card, Typography, TextField, Button, Alert, CircularProgress } from '@mui/material';
import api, { extractErrorMessage } from '../lib/api';
import { defaultShopId } from '../stores/shopStore';

export default function SettingsPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const [vatRate, setVatRate] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['settings', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: { settingKey: string; settingValue: string }[] }>('/settings', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const vat = data?.find((s) => s.settingKey === 'VAT_RATE');
  if (!vatRate && vat) setVatRate(vat.settingValue);

  const save = useMutation({
    mutationFn: async () =>
      api.put('/settings', {
        shopId,
        key: 'VAT_RATE',
        value: vatRate,
      }),
    onSuccess: () => {
      setMessage('Settings saved');
      setError('');
      queryClient.invalidateQueries({ queryKey: ['settings'] });
    },
    onError: (err) => {
      setMessage('');
      setError(extractErrorMessage(err));
    },
  });

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Settings
      </Typography>
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
      <Card sx={{ p: 3, maxWidth: 480 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          VAT Rate
        </Typography>
        {isLoading ? (
          <CircularProgress size={24} />
        ) : (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            <TextField
              label="VAT Rate (%)"
              type="number"
              value={vatRate}
              onChange={(e) => setVatRate(e.target.value)}
              size="small"
            />
            <Button
              variant="contained"
              disabled={!shopId || save.isPending}
              onClick={() => save.mutate()}
            >
              Save
            </Button>
          </Box>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
          This VAT rate is used as the default for new products in this shop.
        </Typography>
      </Card>
    </Box>
  );
}