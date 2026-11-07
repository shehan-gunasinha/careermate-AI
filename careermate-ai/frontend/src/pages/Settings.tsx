import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { KeyRound, LogOut, Trash2 } from 'lucide-react';
import { authApi, geminiKeyApi } from '../api';
import { errorMessage } from '../api/client';
import type { GeminiKeyStatus } from '../api/types';
import { Alert, Badge, ConfirmDialog, PageHeader } from '../components/ui';
import { useAuth } from '../context/AuthContext';

export default function Settings() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [keyStatus, setKeyStatus] = useState<GeminiKeyStatus | null>(null);
  const [keyValue, setKeyValue] = useState('');
  const [keyBusy, setKeyBusy] = useState(false);
  const [keyError, setKeyError] = useState<string | null>(null);
  const [keyNotice, setKeyNotice] = useState<string | null>(null);
  const [confirmRemoveKey, setConfirmRemoveKey] = useState(false);
  const [confirmLogout, setConfirmLogout] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    geminiKeyApi.status().then(setKeyStatus).catch((err) => setKeyError(errorMessage(err)));
  }, []);

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

  const saveKey = async (e: FormEvent) => {
    e.preventDefault();
    const value = keyValue.trim();
    if (!value) return setKeyError('Paste your Gemini API key first.');
    setKeyBusy(true);
    setKeyError(null);
    setKeyNotice(null);
    try {
      setKeyStatus(await geminiKeyApi.save(value));
      setKeyValue('');
      setKeyNotice('Your Gemini API key is saved.');
    } catch (err) {
      setKeyError(errorMessage(err));
    } finally {
      setKeyBusy(false);
    }
  };

  const removeKey = async () => {
    setKeyBusy(true);
    setKeyError(null);
    setKeyNotice(null);
    try {
      setKeyStatus(await geminiKeyApi.remove());
      setKeyNotice('Your Gemini API key was removed.');
    } catch (err) {
      setKeyError(errorMessage(err));
    } finally {
      setKeyBusy(false);
    }
  };

  const doLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
      navigate('/login');
    } finally {
      setLoggingOut(false);
    }
  };

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm({ ...form, [k]: e.target.value });

  const aiEnabled = keyStatus?.aiEnabled ?? false;

  return (
    <div className="max-w-2xl space-y-6">
      <PageHeader title="Settings" subtitle="Account, security and system status." />
      <section className="card space-y-2 p-5">
        <h2 className="font-semibold">Account</h2>
        <p className="text-sm text-slate-600">{user?.fullName} · {user?.email}</p>
        <Badge className="bg-navy-50 text-navy-700">{user?.role === 'ADMIN' ? 'Administrator' : 'Job seeker'}</Badge>
      </section>
      <section className="card space-y-4 p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 font-semibold"><KeyRound className="h-4 w-4" />Gemini API key</h2>
            <p className="mt-1 text-sm text-slate-600">Add your own key from Google AI Studio for resume analysis, job matching and the assistant. It is stored encrypted and never shown in full.</p>
          </div>
          <Badge className={aiEnabled ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-800'}>
            {aiEnabled ? (keyStatus?.configured ? 'AI enabled' : 'Server key active') : 'Fallback mode'}
          </Badge>
        </div>
        {keyError && <Alert>{keyError}</Alert>}
        {keyNotice && <Alert kind="success">{keyNotice}</Alert>}
        <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
          {keyStatus === null
            ? 'Checking your key status...'
            : keyStatus.configured
              ? <>Your key is saved <span className="font-mono font-medium text-slate-800">{keyStatus.hint}</span>. Updating it replaces the previous key immediately.</>
              : keyStatus.serverKeyConfigured
                ? 'No personal key is saved, so the shared server key is being used. Add your own key to use your own Google quota.'
                : 'No Gemini key is configured. Core features still work with built-in fallback logic.'}
        </div>
        <form onSubmit={saveKey} className="space-y-2" noValidate>
          <label className="label" htmlFor="gemini-key">{keyStatus?.configured ? 'Replace key' : 'Add key'}</label>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              id="gemini-key"
              type="password"
              autoComplete="off"
              spellCheck={false}
              className="input flex-1"
              value={keyValue}
              onChange={(e) => setKeyValue(e.target.value)}
              placeholder="Paste your Gemini API key"
            />
            <button className="btn-primary" disabled={keyBusy || !keyValue.trim()}>
              {keyBusy ? 'Saving...' : keyStatus?.configured ? 'Update key' : 'Save key'}
            </button>
          </div>
          <p className="text-xs text-slate-500">
            Get a key from <a className="text-accent-700 underline" href="https://aistudio.google.com/apikey" target="_blank" rel="noreferrer">Google AI Studio</a>. Your browser never stores it; the backend validates and encrypts it before saving.
          </p>
        </form>
        {keyStatus?.configured && (
          <button type="button" className="btn-secondary btn-sm" onClick={() => setConfirmRemoveKey(true)} disabled={keyBusy}>
            <Trash2 className="h-4 w-4" />Remove key
          </button>
        )}
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
      <section className="card p-5"><button className="btn-secondary" onClick={() => setConfirmLogout(true)}><LogOut className="h-4 w-4" />Log out</button></section>

      {confirmRemoveKey && (
        <ConfirmDialog
          title="Remove Gemini API key?"
          message="AI features will fall back to the server key or built-in logic."
          confirmLabel="Remove key"
          busy={keyBusy}
          onConfirm={async () => {
            await removeKey();
            setConfirmRemoveKey(false);
          }}
          onCancel={() => setConfirmRemoveKey(false)}
        />
      )}

      {confirmLogout && (
        <ConfirmDialog
          title="Log out?"
          message="You will need to sign in again to access your CareerMate AI account."
          confirmLabel="Log out"
          busy={loggingOut}
          onConfirm={doLogout}
          onCancel={() => setConfirmLogout(false)}
        />
      )}
    </div>
  );
}
