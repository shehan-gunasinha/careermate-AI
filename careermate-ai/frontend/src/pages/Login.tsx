import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import { errorMessage } from '../api/client';
import { Alert } from '../components/ui';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!email.trim() || !password) {
      setError('Please enter your email and password.');
      return;
    }
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Login failed. Please try again.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <div className="w-full max-w-md">
        <Link to="/" className="mb-6 flex items-center justify-center gap-2 text-xl font-bold text-navy-800">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-navy-700 text-white"><Sparkles className="h-5 w-5" /></span>
          CareerMate <span className="text-accent-600">AI</span>
        </Link>
        <form onSubmit={submit} className="card space-y-4 p-6" noValidate>
          <h1 className="text-xl font-bold">Welcome back</h1>
          {error && <Alert>{error}</Alert>}
          <div>
            <label htmlFor="email" className="label">Email</label>
            <input id="email" type="email" autoComplete="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div>
            <div className="flex items-center justify-between">
              <label htmlFor="password" className="label">Password</label>
              <Link to="/forgot-password" className="mb-1 text-xs font-semibold text-brand-600 hover:underline">Forgot password?</Link>
            </div>
            <input id="password" type="password" autoComplete="current-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button type="submit" className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in...' : 'Log in'}</button>
          <p className="text-center text-sm text-slate-600">New here? <Link to="/register" className="font-semibold text-brand-600">Create an account</Link></p>
        </form>
        <p className="mt-4 rounded-xl bg-white/70 p-3 text-center text-xs text-slate-500">
          Demo account (when demo data is enabled): <code>demo@careermate.local</code> / <code>Demo@12345</code>
        </p>
      </div>
    </div>
  );
}
