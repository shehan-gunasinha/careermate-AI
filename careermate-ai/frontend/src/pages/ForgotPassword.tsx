import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, KeyRound, MailCheck, Sparkles } from 'lucide-react';
import { authApi } from '../api';
import { errorMessage } from '../api/client';
import { Alert } from '../components/ui';

type Step = 'email' | 'reset' | 'done';

export default function ForgotPassword() {
  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const id = window.setTimeout(() => setCooldown((c) => Math.max(0, c - 1)), 1000);
    return () => window.clearTimeout(id);
  }, [cooldown]);

  const requestCode = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setInfo(null);
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) {
      setError('Enter a valid email address.');
      return;
    }
    setBusy(true);
    try {
      await authApi.forgotPassword(email.trim());
      setStep('reset');
      setCooldown(60);
      setInfo('If an account exists for this email, a 6-digit code has been sent. It expires in 10 minutes.');
    } catch (err) {
      setError(errorMessage(err, 'Could not send the reset code. Please try again.'));
    } finally {
      setBusy(false);
    }
  };

  const resendCode = async () => {
    if (cooldown > 0 || busy) return;
    setError(null);
    setInfo(null);
    setBusy(true);
    try {
      await authApi.forgotPassword(email.trim());
      setCooldown(60);
      setInfo('A new reset code has been sent.');
    } catch (err) {
      setError(errorMessage(err, 'Could not resend the reset code. Please try again.'));
    } finally {
      setBusy(false);
    }
  };

  const resetPassword = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!/^\d{6}$/.test(otp.trim())) {
      setError('Enter the 6-digit code from your email.');
      return;
    }
    if (password.length < 8 || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      setError('Password must be at least 8 characters and contain letters and numbers.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setBusy(true);
    try {
      await authApi.resetPassword(email.trim(), otp.trim(), password, confirmPassword);
      setStep('done');
    } catch (err) {
      setError(errorMessage(err, 'Could not reset your password. Please try again.'));
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

        <div className="card space-y-4 p-6">
          {step === 'email' && (
            <>
              <div className="flex items-center gap-2">
                <KeyRound className="h-5 w-5 text-brand-600" aria-hidden />
                <h1 className="text-xl font-bold">Forgot your password?</h1>
              </div>
              <p className="text-sm text-slate-600">Enter your account email and we will send you a 6-digit reset code.</p>
              {error && <Alert>{error}</Alert>}
              <form onSubmit={requestCode} className="space-y-4" noValidate>
                <div>
                  <label htmlFor="email" className="label">Email</label>
                  <input id="email" type="email" autoComplete="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} />
                </div>
                <button type="submit" className="btn-primary w-full" disabled={busy}>{busy ? 'Sending code...' : 'Send reset code'}</button>
              </form>
              <Link to="/login" className="flex items-center justify-center gap-1 text-sm font-semibold text-brand-600"><ArrowLeft className="h-4 w-4" /> Back to login</Link>
            </>
          )}

          {step === 'reset' && (
            <>
              <div className="flex items-center gap-2">
                <MailCheck className="h-5 w-5 text-brand-600" aria-hidden />
                <h1 className="text-xl font-bold">Enter your reset code</h1>
              </div>
              <p className="text-sm text-slate-600">We sent a code to <strong>{email.trim()}</strong>.</p>
              {info && <Alert kind="success">{info}</Alert>}
              {error && <Alert>{error}</Alert>}
              <form onSubmit={resetPassword} className="space-y-4" noValidate>
                <div>
                  <label htmlFor="otp" className="label">6-digit code</label>
                  <input id="otp" type="text" inputMode="numeric" autoComplete="one-time-code" maxLength={6} className="input tracking-[0.4em]" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} />
                </div>
                <div>
                  <label htmlFor="newPassword" className="label">New password</label>
                  <input id="newPassword" type="password" autoComplete="new-password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
                </div>
                <div>
                  <label htmlFor="confirmPassword" className="label">Confirm new password</label>
                  <input id="confirmPassword" type="password" autoComplete="new-password" className="input" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
                </div>
                <button type="submit" className="btn-primary w-full" disabled={busy}>{busy ? 'Resetting password...' : 'Reset password'}</button>
              </form>
              <div className="flex items-center justify-between text-sm">
                <button type="button" className="font-semibold text-brand-600 disabled:text-slate-400" onClick={resendCode} disabled={busy || cooldown > 0}>
                  {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
                </button>
                <button type="button" className="font-semibold text-slate-500" onClick={() => { setStep('email'); setError(null); setInfo(null); }}>Change email</button>
              </div>
            </>
          )}

          {step === 'done' && (
            <>
              <div className="flex items-center gap-2">
                <MailCheck className="h-5 w-5 text-emerald-600" aria-hidden />
                <h1 className="text-xl font-bold">Password reset</h1>
              </div>
              <Alert kind="success">Your password has been changed. You can now log in with your new password.</Alert>
              <Link to="/login" className="btn-primary w-full">Go to login</Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
