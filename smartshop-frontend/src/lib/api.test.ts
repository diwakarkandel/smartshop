import { describe, it, expect } from 'vitest';
import { extractErrorMessage } from './api';

describe('extractErrorMessage', () => {
  it('returns the API message for axios errors', () => {
    const err = {
      isAxiosError: true,
      response: { data: { message: 'Invalid credentials' } },
    } as never;
    expect(extractErrorMessage(err)).toBe('Invalid credentials');
  });

  it('falls back to the plain error message', () => {
    expect(extractErrorMessage(new Error('boom'))).toBe('boom');
  });
});