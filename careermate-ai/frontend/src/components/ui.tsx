import { useEffect, useState, type KeyboardEvent, type ReactNode } from 'react';
import { AlertCircle, CheckCircle2, ChevronLeft, ChevronRight, Loader2, X } from 'lucide-react';
import { scoreBg, scoreColor } from '../lib/format';

export function Spinner({ label = 'Loading...' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center justify-center gap-2 py-10 text-slate-500">
      <Loader2 className="h-5 w-5 animate-spin" aria-hidden />
      <span className="text-sm">{label}</span>
    </div>
  );
}

export function Alert({ kind = 'error', children }: { kind?: 'error' | 'success' | 'info'; children: ReactNode }) {
  const styles = {
    error: 'border-red-200 bg-red-50 text-red-800',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
    info: 'border-blue-200 bg-blue-50 text-blue-800',
  }[kind];
  const Icon = kind === 'success' ? CheckCircle2 : AlertCircle;
  return (
    <div role={kind === 'error' ? 'alert' : 'status'} className={`flex items-start gap-2 rounded-xl border px-3 py-2 text-sm ${styles}`}>
      <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <div>{children}</div>
    </div>
  );
}

export function Badge({ children, className = 'bg-slate-100 text-slate-700' }: { children: ReactNode; className?: string }) {
  return <span className={`badge ${className}`}>{children}</span>;
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function EmptyState({ icon, title, text, action }: { icon?: ReactNode; title: string; text?: string; action?: ReactNode }) {
  return (
    <div className="card flex flex-col items-center gap-2 px-6 py-12 text-center">
      {icon && <div className="text-slate-300">{icon}</div>}
      <h3 className="font-semibold">{title}</h3>
      {text && <p className="max-w-md text-sm text-slate-500">{text}</p>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}

export function ProgressBar({ value, className = '' }: { value: number; className?: string }) {
  const v = Math.max(0, Math.min(100, value));
  return (
    <div className={`h-2.5 w-full overflow-hidden rounded-full bg-slate-100 ${className}`} role="progressbar" aria-valuenow={v} aria-valuemin={0} aria-valuemax={100}>
      <div className={`h-full rounded-full ${scoreBg(v)} transition-all`} style={{ width: `${v}%` }} />
    </div>
  );
}

export function ScoreRing({ value, size = 88, label }: { value: number; size?: number; label?: string }) {
  const v = Math.max(0, Math.min(100, Math.round(value)));
  const r = (size - 10) / 2;
  const c = 2 * Math.PI * r;
  return (
    <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }} aria-label={`${label ?? 'Score'} ${v} percent`}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} strokeWidth="8" className="fill-none stroke-slate-100" />
        <circle
          cx={size / 2} cy={size / 2} r={r} strokeWidth="8" strokeLinecap="round"
          className={`fill-none ${v >= 75 ? 'stroke-emerald-500' : v >= 50 ? 'stroke-amber-500' : 'stroke-red-500'}`}
          strokeDasharray={c} strokeDashoffset={c - (c * v) / 100}
        />
      </svg>
      <span className={`absolute text-lg font-bold ${scoreColor(v)}`}>{v}%</span>
    </div>
  );
}

export function StatCard({ label, value, icon, tone = 'text-navy-700 bg-navy-50' }: { label: string; value: ReactNode; icon: ReactNode; tone?: string }) {
  return (
    <div className="card flex items-center gap-4 p-4">
      <div className={`flex h-11 w-11 items-center justify-center rounded-xl ${tone}`}>{icon}</div>
      <div>
        <div className="text-2xl font-bold leading-tight">{value}</div>
        <div className="text-xs font-medium text-slate-500">{label}</div>
      </div>
    </div>
  );
}

export function Modal({ title, onClose, children, wide = false }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-slate-900/50 p-4" role="dialog" aria-modal="true" aria-label={title}>
      <div className={`card my-8 w-full ${wide ? 'max-w-3xl' : 'max-w-xl'} p-5`}>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button type="button" className="btn-ghost btn-sm" onClick={onClose} aria-label="Close dialog"><X className="h-4 w-4" /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function ConfirmDialog({
  title, message, confirmLabel = 'Confirm', cancelLabel = 'Cancel', tone = 'danger', busy = false, onConfirm, onCancel,
}: {
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: 'danger' | 'primary';
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  useEffect(() => {
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.key === 'Escape' && !busy) onCancel();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [busy, onCancel]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busy) onCancel();
      }}
    >
      <div className="card w-full max-w-sm p-5 shadow-xl" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
        <h2 id="confirm-dialog-title" className="text-lg font-semibold">{title}</h2>
        <div className="mt-2 text-sm text-slate-600">{message}</div>
        <div className="mt-5 flex justify-end gap-2">
          <button type="button" className="btn-secondary" onClick={onCancel} disabled={busy} autoFocus>{cancelLabel}</button>
          <button type="button" className={tone === 'danger' ? 'btn-danger' : 'btn-primary'} onClick={onConfirm} disabled={busy}>
            {busy ? 'Working...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="mt-6 flex items-center justify-center gap-3" aria-label="Pagination">
      <button className="btn-secondary btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft className="h-4 w-4" /> Previous
      </button>
      <span className="text-sm text-slate-600">Page {page + 1} of {totalPages}</span>
      <button className="btn-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next <ChevronRight className="h-4 w-4" />
      </button>
    </nav>
  );
}

export function TagInput({ value, onChange, placeholder, id }: { value: string[]; onChange: (v: string[]) => void; placeholder?: string; id?: string }) {
  const [text, setText] = useState('');
  const add = (raw: string) => {
    const t = raw.trim();
    if (t && !value.some((v) => v.toLowerCase() === t.toLowerCase())) onChange([...value, t]);
    setText('');
  };
  const onKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      add(text);
    } else if (e.key === 'Backspace' && !text && value.length) {
      onChange(value.slice(0, -1));
    }
  };
  return (
    <div className="flex flex-wrap items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-2 py-1.5 focus-within:border-accent-600 focus-within:ring-2 focus-within:ring-accent-100">
      {value.map((t) => (
        <span key={t} className="inline-flex items-center gap-1 rounded-full bg-navy-50 px-2.5 py-0.5 text-xs font-medium text-navy-700">
          {t}
          <button type="button" aria-label={`Remove ${t}`} onClick={() => onChange(value.filter((v) => v !== t))}><X className="h-3 w-3" /></button>
        </span>
      ))}
      <input
        id={id} className="min-w-[8rem] flex-1 bg-transparent px-1 py-1 text-sm outline-none" value={text} placeholder={value.length ? '' : placeholder}
        onChange={(e) => setText(e.target.value)} onKeyDown={onKey} onBlur={() => text && add(text)}
      />
    </div>
  );
}
