import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ForgotPassword from '../pages/ForgotPassword';

vi.mock('../api', () => ({ authApi: { forgotPassword: vi.fn(), resetPassword: vi.fn() } }));
import { authApi } from '../api';

const renderPage = () =>
  render(
    <MemoryRouter>
      <ForgotPassword />
    </MemoryRouter>,
  );

describe('Forgot password page', () => {
  beforeEach(() => vi.clearAllMocks());

  it('requests a code and shows the reset form', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValue(null);
    renderPage();
    await userEvent.type(screen.getByLabelText('Email'), 'ann@test.local');
    await userEvent.click(screen.getByRole('button', { name: /send reset code/i }));
    expect(await screen.findByLabelText('6-digit code')).toBeInTheDocument();
    expect(authApi.forgotPassword).toHaveBeenCalledWith('ann@test.local');
  });

  it('resets the password and shows confirmation', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValue(null);
    vi.mocked(authApi.resetPassword).mockResolvedValue(null);
    renderPage();
    await userEvent.type(screen.getByLabelText('Email'), 'ann@test.local');
    await userEvent.click(screen.getByRole('button', { name: /send reset code/i }));
    await userEvent.type(await screen.findByLabelText('6-digit code'), '123456');
    await userEvent.type(screen.getByLabelText('New password'), 'NewPassw0rd');
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'NewPassw0rd');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/your password has been changed/i)).toBeInTheDocument();
    expect(authApi.resetPassword).toHaveBeenCalledWith('ann@test.local', '123456', 'NewPassw0rd', 'NewPassw0rd');
  });
});
