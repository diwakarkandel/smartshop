import { useNavigate } from 'react-router-dom';
import { Box, Button, Card, Typography } from '@mui/material';
import GppBadOutlinedIcon from '@mui/icons-material/GppBadOutlined';

export default function NotAuthorizedPage() {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: '#f5f6f8',
      }}
    >
      <Card sx={{ p: 4, maxWidth: 420, textAlign: 'center' }}>
        <GppBadOutlinedIcon sx={{ fontSize: 56, color: 'error.main', mb: 2 }} />
        <Typography variant="h5" sx={{ mb: 1, fontWeight: 600 }}>
          Not Authorized
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          You do not have permission to view this page. Contact your shop administrator
          if you believe this is a mistake.
        </Typography>
        <Button variant="contained" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </Button>
      </Card>
    </Box>
  );
}