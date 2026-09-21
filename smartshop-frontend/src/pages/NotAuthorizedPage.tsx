import { useNavigate } from 'react-router-dom';
import { Box, Button, Card, Typography, Stack } from '@mui/material';
import GppBadOutlinedIcon from '@mui/icons-material/GppBadOutlined';
import StorefrontIcon from '@mui/icons-material/Storefront';
import GroupAddIcon from '@mui/icons-material/GroupAdd';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuthStore } from '../stores/authStore';
import { getDefaultRoute } from '../lib/routeRoles';

export default function NotAuthorizedPage() {
  const navigate = useNavigate();
  const { user, setSession } = useAuthStore();

  const hasNoRoles = !user?.roles || user.roles.length === 0;

  const handleSignOut = () => {
    setSession(null);
    navigate('/login');
  };

  if (hasNoRoles) {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: '#1B4332',
          p: 2,
        }}
      >
        <Card sx={{ p: 4, maxWidth: 460, textAlign: 'center', borderRadius: 2 }}>
          <StorefrontIcon sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
          <Typography variant="h5" sx={{ mb: 1, fontWeight: 700 }}>
            Welcome to SmartShop!
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Your account is created and ready. You are not currently assigned to a shop.
            You can either register your own shop to get started as an administrator, or join
            an existing shop with a staff invite code.
          </Typography>
          <Stack spacing={1.5}>
            <Button
              variant="contained"
              size="large"
              startIcon={<StorefrontIcon />}
              onClick={() => navigate('/shop-registration')}
            >
              Register a New Shop
            </Button>
            <Button
              variant="outlined"
              size="large"
              startIcon={<GroupAddIcon />}
              onClick={() => navigate('/join-shop')}
            >
              Join Existing Shop
            </Button>
            <Button
              variant="text"
              color="inherit"
              size="small"
              startIcon={<LogoutIcon />}
              onClick={handleSignOut}
              sx={{ mt: 1 }}
            >
              Sign Out
            </Button>
          </Stack>
        </Card>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: '#f5f6f8',
        p: 2,
      }}
    >
      <Card sx={{ p: 4, maxWidth: 420, textAlign: 'center', borderRadius: 2 }}>
        <GppBadOutlinedIcon sx={{ fontSize: 56, color: 'error.main', mb: 2 }} />
        <Typography variant="h5" sx={{ mb: 1, fontWeight: 600 }}>
          Not Authorized
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          You do not have permission to view this specific page. Contact your shop administrator
          if you believe this is a mistake.
        </Typography>
        <Button variant="contained" onClick={() => navigate(getDefaultRoute(user?.roles))}>
          Back to Your Dashboard
        </Button>
      </Card>
    </Box>
  );
}