import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Card, Chip, Container, Grid, Stack, Typography } from '@mui/material';
import StorefrontIcon from '@mui/icons-material/Storefront';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import InsightsIcon from '@mui/icons-material/Insights';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import ShieldIcon from '@mui/icons-material/Shield';
import PercentIcon from '@mui/icons-material/Percent';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';

const FEATURES = [
  { icon: <PointOfSaleIcon />, title: 'Point of Sale', desc: 'Fast billing with live tax, discounts, multiple payment methods and printable invoices.' },
  { icon: <Inventory2Icon />, title: 'Inventory & Stock', desc: 'Real-time stock, weighted-average costing, low-stock and slow-moving alerts.' },
  { icon: <ShoppingCartIcon />, title: 'Purchasing', desc: 'Record purchases, suppliers, purchase returns and supplier payments in one place.' },
  { icon: <SwapHorizIcon />, title: 'Multi-Branch Transfers', desc: 'Move stock between branches with an approval workflow and full audit trail.' },
  { icon: <InsightsIcon />, title: 'Reports & Analytics', desc: 'Sales, profit-by-category, top products and inventory valuation with CSV export.' },
  { icon: <PercentIcon />, title: 'Tax (VAT) Management', desc: 'Per-shop tax configuration with effective-dated rates applied at checkout.' },
  { icon: <AccountTreeIcon />, title: 'Multi-Shop & Roles', desc: 'Run many shops and branches with six role levels, from cashier to platform admin.' },
  { icon: <ShieldIcon />, title: 'Audit & Security', desc: 'Tenant isolation and audit logging on every sensitive action — who changed what, when.' },
];

function GradientIcon({ children }: { children: React.ReactNode }) {
  return (
    <Box
      sx={{
        width: 48,
        height: 48,
        borderRadius: 2.5,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#fff',
        background: 'linear-gradient(135deg, #1B4332 0%, #40916C 100%)',
        boxShadow: '0 6px 16px rgba(27,67,50,0.28)',
      }}
    >
      {children}
    </Box>
  );
}

export default function LandingPage() {
  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* ---- Top bar ---- */}
      <Box
        component="header"
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 10,
          backdropFilter: 'blur(10px)',
          backgroundColor: 'rgba(255,255,255,0.8)',
          borderBottom: 1,
          borderColor: 'divider',
        }}
      >
        <Container maxWidth="lg" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', py: 1.5 }}>
          <Stack direction="row" spacing={1.25} alignItems="center">
            <GradientIcon><StorefrontIcon /></GradientIcon>
            <Typography variant="h6" fontWeight={800}>SmartShop</Typography>
          </Stack>
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/login">Sign in</Button>
            <Button component={RouterLink} to="/register" variant="contained">Get started</Button>
          </Stack>
        </Container>
      </Box>

      {/* ---- Hero ---- */}
      <Box
        sx={{
          position: 'relative',
          overflow: 'hidden',
          color: '#fff',
          background: 'linear-gradient(135deg, #12362A 0%, #1B4332 45%, #2D6A4F 100%)',
          '&::before': {
            content: '""',
            position: 'absolute',
            inset: 0,
            background:
              'radial-gradient(700px 400px at 12% 15%, rgba(64,145,108,0.45), transparent 60%),' +
              'radial-gradient(600px 400px at 88% 85%, rgba(232,223,202,0.18), transparent 60%)',
          },
        }}
      >
        <Container maxWidth="lg" sx={{ position: 'relative', py: { xs: 7, md: 12 } }}>
          <Box sx={{ maxWidth: 760 }} className="ss-page">
            <Chip
              label="Retail Management Platform"
              sx={{ mb: 2.5, color: '#fff', bgcolor: 'rgba(255,255,255,0.14)', fontWeight: 600 }}
            />
            <Typography variant="h3" fontWeight={800} sx={{ letterSpacing: '-0.02em', lineHeight: 1.1, mb: 2 }}>
              Run your shop end-to-end — sales, stock, and books in one place.
            </Typography>
            <Typography variant="h6" sx={{ fontWeight: 400, opacity: 0.9, mb: 4 }}>
              SmartShop is a multi-branch <strong>retail POS, inventory and accounting</strong> system.
              Sell at the counter, track every unit of stock across branches, manage purchases and
              suppliers, and see real profit — with role-based access and a full audit trail.
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
              <Button component={RouterLink} to="/register" variant="contained" size="large" sx={{ px: 4 }}>
                Get started free
              </Button>
              <Button
                component={RouterLink}
                to="/login"
                size="large"
                sx={{ px: 4, color: '#fff', borderColor: 'rgba(255,255,255,0.5)' }}
                variant="outlined"
              >
                Sign in
              </Button>
            </Stack>
          </Box>
        </Container>
      </Box>

      {/* ---- What it is ---- */}
      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 9 } }}>
        <Box sx={{ textAlign: 'center', mb: 5 }}>
          <Typography variant="overline" color="primary" fontWeight={700}>What is SmartShop?</Typography>
          <Typography variant="h4" fontWeight={800} sx={{ mb: 1.5 }}>
            One system for the whole retail operation
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 720, mx: 'auto' }}>
            Purpose-built for shops and small retail chains — from a single counter to many branches
            under many shops. Everything below works together on the same live data.
          </Typography>
        </Box>

        <Grid container spacing={3} className="ss-stagger">
          {FEATURES.map((f) => (
            <Grid item xs={12} sm={6} md={3} key={f.title}>
              <Card sx={{ p: 3, height: '100%' }}>
                <GradientIcon>{f.icon}</GradientIcon>
                <Typography variant="subtitle1" fontWeight={700} sx={{ mt: 2, mb: 0.5 }}>
                  {f.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {f.desc}
                </Typography>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      {/* ---- CTA band ---- */}
      <Box sx={{ bgcolor: 'rgba(27,67,50,0.04)', borderTop: 1, borderColor: 'divider' }}>
        <Container maxWidth="md" sx={{ py: { xs: 6, md: 8 }, textAlign: 'center' }}>
          <Typography variant="h4" fontWeight={800} sx={{ mb: 1.5 }}>
            Ready to organize your shop?
          </Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
            Create your account, register your shop, and start selling in minutes.
          </Typography>
          <Button component={RouterLink} to="/register" variant="contained" size="large" sx={{ px: 5 }}>
            Get started
          </Button>
        </Container>
      </Box>

      {/* ---- Footer ---- */}
      <Box component="footer" sx={{ mt: 'auto', py: 3, borderTop: 1, borderColor: 'divider' }}>
        <Container maxWidth="lg" sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, justifyContent: 'space-between', alignItems: 'center' }}>
          <Stack direction="row" spacing={1} alignItems="center">
            <StorefrontIcon fontSize="small" color="primary" />
            <Typography variant="body2" color="text.secondary">
              SmartShop — Retail POS, Inventory & Accounting
            </Typography>
          </Stack>
          <Typography variant="caption" color="text.secondary">
            © {new Date().getFullYear()} SmartShop
          </Typography>
        </Container>
      </Box>
    </Box>
  );
}
