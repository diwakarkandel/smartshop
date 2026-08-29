import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#1B4332' },
    secondary: { main: '#2D6A4F' },
    background: { default: '#F8F7F4', paper: '#FFFFFF' },
    text: { primary: '#475569', secondary: '#64748B' },
    success: { main: '#2E7D32' },
    warning: { main: '#B45309' },
    error: { main: '#B91C1C' },
    info: { main: '#E8DFCA' },
  },
  typography: {
    fontFamily: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'].join(','),
    h4: { fontWeight: 700 },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 600 },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiCard: { defaultProps: { variant: 'outlined' } },
  },
});

export default theme;