import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import LoginPage from './LoginPage';
import theme from '../theme';

describe('LoginPage', () => {
  it('renders the sign-in form', () => {
    render(
      <MemoryRouter>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <LoginPage />
        </ThemeProvider>
      </MemoryRouter>,
    );
    expect(screen.getByText('SmartShop')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });
});