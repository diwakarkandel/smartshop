import { createTheme, alpha } from '@mui/material/styles';

// ------------------------------------------------------------------ //
// Brand palette — a refined emerald/forest system with a warm sand
// accent. Gradients are reused across the shell, buttons and avatars.
// ------------------------------------------------------------------ //
const BRAND = {
  deep: '#12362A',
  main: '#1B4332',
  mid: '#2D6A4F',
  bright: '#40916C',
  sand: '#E8DFCA',
  sandDeep: '#C9A96A',
};

export const GRADIENTS = {
  brand: `linear-gradient(135deg, ${BRAND.main} 0%, ${BRAND.mid} 55%, ${BRAND.bright} 100%)`,
  sidebar: `linear-gradient(180deg, ${BRAND.deep} 0%, ${BRAND.main} 55%, #163B2C 100%)`,
  sand: `linear-gradient(135deg, ${BRAND.sand} 0%, ${BRAND.sandDeep} 100%)`,
};

const SOFT_SHADOW = '0 1px 2px rgba(16, 40, 30, 0.04), 0 8px 24px rgba(16, 40, 30, 0.06)';
const HOVER_SHADOW = '0 6px 16px rgba(16, 40, 30, 0.10), 0 18px 40px rgba(16, 40, 30, 0.12)';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: BRAND.main, light: BRAND.mid, dark: BRAND.deep, contrastText: '#fff' },
    secondary: { main: BRAND.bright, light: '#74C69D', dark: BRAND.mid, contrastText: '#fff' },
    background: { default: '#EAF0EC', paper: '#FFFFFF' },
    text: { primary: '#1F2A25', secondary: '#5B6B63' },
    success: { main: '#2E7D32' },
    warning: { main: '#B45309' },
    error: { main: '#B91C1C' },
    info: { main: BRAND.mid },
    divider: 'rgba(16, 40, 30, 0.10)',
  },
  typography: {
    fontFamily: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'].join(','),
    h4: { fontWeight: 800, letterSpacing: '-0.02em' },
    h5: { fontWeight: 800, letterSpacing: '-0.01em' },
    h6: { fontWeight: 700, letterSpacing: '-0.01em' },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 700 },
    button: { fontWeight: 600, textTransform: 'none', letterSpacing: 0 },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        '::selection': { background: alpha(BRAND.bright, 0.25) },
      },
    },
    MuiCard: {
      defaultProps: { variant: 'outlined' },
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: '1px solid rgba(16, 40, 30, 0.06)',
          // A whisper-soft surface (not flat white) so cards read as raised panels.
          backgroundImage: 'linear-gradient(180deg, #FFFFFF 0%, #FBFDFB 100%)',
          boxShadow: SOFT_SHADOW,
          overflow: 'hidden',
          transition: 'transform 0.25s cubic-bezier(0.22,1,0.36,1), box-shadow 0.25s cubic-bezier(0.22,1,0.36,1), border-color 0.25s',
          '&:hover': {
            boxShadow: HOVER_SHADOW,
            borderColor: alpha(BRAND.bright, 0.35),
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: { borderRadius: 16 },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: 10,
          paddingInline: 16,
          transition: 'transform 0.18s cubic-bezier(0.22,1,0.36,1), box-shadow 0.18s, background 0.25s, border-color 0.2s',
          '&:hover': { transform: 'translateY(-1px)' },
          '&:active': { transform: 'translateY(0)' },
          '&.Mui-disabled': { transform: 'none' },
        },
        containedPrimary: {
          background: GRADIENTS.brand,
          boxShadow: '0 4px 12px rgba(27, 67, 50, 0.24)',
          '&:hover': { boxShadow: '0 8px 20px rgba(27, 67, 50, 0.32)' },
        },
        outlined: {
          borderColor: 'rgba(16, 40, 30, 0.18)',
          '&:hover': { borderColor: BRAND.mid, backgroundColor: alpha(BRAND.bright, 0.06) },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { fontWeight: 600, borderRadius: 8 },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            backgroundColor: alpha(BRAND.bright, 0.06),
            color: '#1F2A25',
            fontWeight: 700,
            fontSize: 12,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            borderBottom: `1px solid ${alpha(BRAND.mid, 0.16)}`,
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          transition: 'background-color 0.18s ease',
          '&:hover': { backgroundColor: alpha(BRAND.bright, 0.05) },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: 'rgba(16, 40, 30, 0.07)' },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition: 'box-shadow 0.2s ease',
          '&.Mui-focused': { boxShadow: `0 0 0 3px ${alpha(BRAND.bright, 0.18)}` },
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 18,
          boxShadow: '0 24px 60px rgba(16, 40, 30, 0.22)',
          animation: 'ss-fade-in-scale 0.28s cubic-bezier(0.22,1,0.36,1) both',
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backdropFilter: 'blur(10px)',
          backgroundColor: 'rgba(255,255,255,0.78)',
        },
      },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition: 'background-color 0.2s ease, transform 0.2s ease, color 0.2s ease',
        },
      },
    },
    MuiAvatar: {
      styleOverrides: {
        root: { fontWeight: 700 },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: { borderRadius: 8, fontSize: 12, backgroundColor: BRAND.deep },
      },
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 8, height: 6 },
      },
    },
  },
});

export default theme;
