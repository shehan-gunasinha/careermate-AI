import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Login from '../pages/Login';
import { AuthProvider } from '../context/AuthContext';

vi.mock('../api', () => ({
  authApi: { login: vi.fn(), register: vi.fn(), me: vi.fn(), logout: vi.fn() },
}));
import { authApi } from '../api';

const renderLogin = () =>
  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );

describe('Login page', () => {
  beforeEach(() => vi.clearAllMocks());

  it('validates empty input without calling the API', async () => {
    renderLogin();
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/enter your email and password/i);
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it('logs in, stores the token and navigates to the dashboard', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'jwt-token', tokenType: 'Bearer', expiresInSeconds: 3600,
      user: { id: 1, fullName: 'Ann Lee', email: 'ann@test.local', role: 'JOB_SEEKER', createdAt: '' },
    });
    renderLogin();
    await userEvent.type(screen.getByLabelText(/email/i), 'ann@test.local');
    await userEvent.type(screen.getByLabelText(/password/i), 'Passw0rdTest');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText('Dashboard page')).toBeInTheDocument();
    expect(localStorage.getItem('careermate_token')).toBe('jwt-token');
  });

  it('shows the server error for bad credentials', async () => {
    const err = Object.assign(new Error('bad'), { isAxiosError: true, response: { status: 401, data: { message: 'Invalid email or password' } } });
    vi.mocked(authApi.login).mockRejectedValue(err);
    renderLogin();
    await userEvent.type(screen.getByLabelText(/email/i), 'ann@test.local');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent(/invalid email or password/i));
  });
});
