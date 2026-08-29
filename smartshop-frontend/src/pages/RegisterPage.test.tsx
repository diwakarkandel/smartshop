import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import RegisterPage from './RegisterPage';
import theme from '../theme';

describe('RegisterPage', () => {
  it('renders the sign-up form', () => {
    render(
      <MemoryRouter>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RegisterPage />
        </ThemeProvider>
      </MemoryRouter>,
    );
    expect(screen.getByText('SmartShop')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign up/i })).toBeInTheDocument();
  });
});