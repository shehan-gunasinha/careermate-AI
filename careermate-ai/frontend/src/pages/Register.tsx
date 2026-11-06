import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import { errorMessage, fieldErrors } from '../api/client';
import { Alert } from '../components/ui';
import { useAuth } from '../context/AuthContext';

export function validateRegistration(v: { fullName: string; email: string; password: string; confirmPassword: string }): Record<string, string> {
  const errs: Record<string, string> = {};
  if (!v.fullName.trim()) errs.fullName = 'Full name is required';
  if (!/^\S+@\S+\.\S+$/.test(v.email.trim())) errs.email = 'Enter a valid email address';
  if (v.password.length < 8) errs.password = 'Password must be at least 8 characters';
  else if (!/[A-Za-z]/.test(v.password) || !/\d/.test(v.password)) errs.password = 'Password must contain letters and numbers';
  if (v.confirmPassword !== v.password) errs.confirmPassword = 'Passwords do not match';
  return errs;
}

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    const v = validateRegistration(form);
    setErrors(v);
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      await register(form.fullName.trim(), form.email.trim(), form.password, form.confirmPassword);
      navigate('/profile', { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err, 'Registration failed.'));
    } finally {
      setBusy(false);
    }
  };

  const field = (id: keyof typeof form, label: string, type = 'text', autoComplete?: string) => (
    <div>
      <label htmlFor={id} className="label">{label}</label>
      <input id={id} type={type} autoComplete={autoComplete} className="input" value={form[id]} onChange={set(id)} aria-invalid={!!errors[id]} />
      {errors[id] && <p className="field-error">{errors[id]}</p>}
    </div>
  );

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <div className="w-full max-w-md">
        <Link to="/" className="mb-6 flex items-center justify-center gap-2 text-xl font-bold text-navy-800">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-navy-700 text-white"><Sparkles className="h-5 w-5" /></span>
          CareerMate <span className="text-accent-600">AI</span>
        </Link>
        <form onSubmit={submit} className="card space-y-4 p-6" noValidate>
          <h1 className="text-xl font-bold">Create your account</h1>
          {error && <Alert>{error}</Alert>}
          {field('fullName', 'Full name', 'text', 'name')}
          {field('email', 'Email', 'email', 'email')}
          {field('password', 'Password', 'password', 'new-password')}
          {field('confirmPassword', 'Confirm password', 'password', 'new-password')}
          <button type="submit" className="btn-primary w-full" disabled={busy}>{busy ? 'Creating account...' : 'Create account'}</button>
          <p className="text-center text-sm text-slate-600">Already registered? <Link to="/login" className="font-semibold text-brand-600">Log in</Link></p>
        </form>
      </div>
    </div>
  );
}
