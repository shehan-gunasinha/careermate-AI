import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { assistantApi, authApi } from '../api';
import { errorMessage } from '../api/client';
import { Alert, Badge, PageHeader } from '../components/ui';
import { useAuth } from '../context/AuthContext';

export default function Settings() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [aiEnabled, setAiEnabled] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => { assistantApi.status().then((s) => setAiEnabled(s.aiEnabled)).catch(() => setAiEnabled(false)); }, []);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setNotice(null);
    if (form.newPassword.length < 8 || !/[A-Za-z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) return setError('New password must be at least 8 characters with letters and numbers.');
    if (form.newPassword !== form.confirmPassword) return setError('Passwords do not match.');
    setBusy(true);
    try {
      await authApi.changePassword(form.currentPassword, form.newPassword, form.confirmPassword);
      setNotice('Password changed.');
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm({ ...form, [k]: e.target.value });

  return (
    <div className="max-w-2xl space-y-6">
      <PageHeader title="Settings" subtitle="Account, security and system status." />
      <section className="card space-y-2 p-5">
        <h2 className="font-semibold">Account</h2>
        <p className="text-sm text-slate-600">{user?.fullName} · {user?.email}</p>
        <Badge className="bg-navy-50 text-navy-700">{user?.role === 'ADMIN' ? 'Administrator' : 'Job seeker'}</Badge>
      </section>
      <section className="card space-y-2 p-5">
        <h2 className="font-semibold">AI features</h2>
        <p className="text-sm text-slate-600">{aiEnabled === null ? 'Checking...' : aiEnabled ? 'Google Gemini is configured. Resume analysis, job matching and the assistant use AI.' : 'No Gemini API key is configured on the server. Core features work with built-in fallback logic.'}</p>
        <Badge className={aiEnabled ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-800'}>{aiEnabled ? 'AI enabled' : 'Fallback mode'}</Badge>
      </section>
      <form onSubmit={submit} className="card space-y-3 p-5" noValidate>
        <h2 className="font-semibold">Change password</h2>
        {error && <Alert>{error}</Alert>}
        {notice && <Alert kind="success">{notice}</Alert>}
        <div><label className="label" htmlFor="cp-cur">Current password</label><input id="cp-cur" type="password" autoComplete="current-password" className="input" value={form.currentPassword} onChange={set('currentPassword')} /></div>
        <div><label className="label" htmlFor="cp-new">New password</label><input id="cp-new" type="password" autoComplete="new-password" className="input" value={form.newPassword} onChange={set('newPassword')} /></div>
        <div><label className="label" htmlFor="cp-conf">Confirm new password</label><input id="cp-conf" type="password" autoComplete="new-password" className="input" value={form.confirmPassword} onChange={set('confirmPassword')} /></div>
        <button className="btn-primary" disabled={busy}>{busy ? 'Saving...' : 'Update password'}</button>
      </form>
      <section className="card p-5"><button className="btn-secondary" onClick={async () => { await logout(); navigate('/login'); }}><LogOut className="h-4 w-4" />Log out</button></section>
    </div>
  );
}
