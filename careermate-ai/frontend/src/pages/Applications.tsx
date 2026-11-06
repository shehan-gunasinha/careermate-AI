import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { ExternalLink, History, LayoutGrid, Pencil, Plus, Table2, Trash2 } from 'lucide-react';
import { applicationApi } from '../api';
import { errorMessage } from '../api/client';
import type { Application, ApplicationHistory, ApplicationInput, ApplicationStatus } from '../api/types';
import { Alert, EmptyState, Modal, PageHeader, Spinner } from '../components/ui';
import { APPLICATION_STATUSES, formatDate, formatDateTime, statusMeta } from '../lib/format';

const blank = (): ApplicationInput => ({ jobTitle: '', company: '', status: 'APPLIED', applicationDate: new Date().toISOString().slice(0, 10), applicationUrl: '', contactPerson: '', interviewDate: '', notes: '' });

export default function Applications() {
  const [items, setItems] = useState<Application[] | null>(null);
  const [view, setView] = useState<'table' | 'kanban'>('kanban');
  const [filter, setFilter] = useState<ApplicationStatus | ''>('');
  const [q, setQ] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<{ id?: number; data: ApplicationInput } | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [history, setHistory] = useState<{ app: Application; rows: ApplicationHistory[] } | null>(null);
  const [toDelete, setToDelete] = useState<Application | null>(null);
  const [dragId, setDragId] = useState<number | null>(null);

  const load = useCallback(() => {
    applicationApi.list({ status: filter || undefined, q: q.trim() || undefined }).then((p) => setItems(p.content)).catch((e) => setError(errorMessage(e)));
  }, [filter, q]);
  useEffect(() => { const t = window.setTimeout(load, 250); return () => window.clearTimeout(t); }, [load]);

  const changeStatus = async (a: Application, status: ApplicationStatus) => {
    if (a.status === status) return;
    try {
      const upd = await applicationApi.setStatus(a.id, status);
      setItems((list) => list && list.map((x) => (x.id === a.id ? upd : x)));
      window.dispatchEvent(new Event('notifications:changed'));
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const save = async (e: FormEvent) => {
    e.preventDefault();
    if (!editing) return;
    const d = editing.data;
    if (!d.jobTitle?.trim() || !d.company?.trim()) return setFormError('Job title and company are required.');
    setFormError(null);
    const payload: ApplicationInput = { ...d, applicationDate: d.applicationDate || null, interviewDate: d.interviewDate || null, applicationUrl: d.applicationUrl || null };
    try {
      if (editing.id) await applicationApi.update(editing.id, payload);
      else await applicationApi.create(payload);
      setEditing(null);
      load();
      window.dispatchEvent(new Event('notifications:changed'));
    } catch (err) {
      setFormError(errorMessage(err));
    }
  };

  const edit = (a: Application) => setEditing({
    id: a.id,
    data: { jobId: a.jobId, jobTitle: a.jobTitle, company: a.company, status: a.status, applicationDate: a.applicationDate ?? '', applicationUrl: a.applicationUrl ?? '', contactPerson: a.contactPerson ?? '', interviewDate: a.interviewDate?.slice(0, 16) ?? '', notes: a.notes ?? '' },
  });

  const showHistory = async (a: Application) => {
    try {
      setHistory({ app: a, rows: await applicationApi.history(a.id) });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const remove = async () => {
    if (!toDelete) return;
    try {
      await applicationApi.remove(toDelete.id);
      setToDelete(null);
      load();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const grouped = useMemo(() => {
    const m = new Map<string, Application[]>();
    APPLICATION_STATUSES.forEach((s) => m.set(s.value, []));
    (items ?? []).forEach((a) => m.get(a.status)?.push(a));
    return m;
  }, [items]);

  const setField = (k: keyof ApplicationInput) => (e: { target: { value: string } }) => editing && setEditing({ ...editing, data: { ...editing.data, [k]: e.target.value } });

  return (
    <div>
      <PageHeader title="Application Tracker" subtitle="Keep track of every application, interview and outcome."
        actions={<button className="btn-accent" onClick={() => { setFormError(null); setEditing({ data: blank() }); }}><Plus className="h-4 w-4" />Add application</button>} />
      {error && <div className="mb-4"><Alert>{error}</Alert></div>}

      <div className="card mb-4 flex flex-wrap items-center gap-3 p-3">
        <input aria-label="Search applications" className="input max-w-xs" placeholder="Search title or company" value={q} onChange={(e) => setQ(e.target.value)} />
        <select aria-label="Filter by status" className="input max-w-[14rem]" value={filter} onChange={(e) => setFilter(e.target.value as ApplicationStatus | '')}>
          <option value="">All statuses</option>{APPLICATION_STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
        <div className="ml-auto flex gap-1" role="group" aria-label="View mode">
          <button className={view === 'kanban' ? 'btn-primary btn-sm' : 'btn-secondary btn-sm'} onClick={() => setView('kanban')} aria-pressed={view === 'kanban'}><LayoutGrid className="h-4 w-4" />Board</button>
          <button className={view === 'table' ? 'btn-primary btn-sm' : 'btn-secondary btn-sm'} onClick={() => setView('table')} aria-pressed={view === 'table'}><Table2 className="h-4 w-4" />Table</button>
        </div>
      </div>

      {!items ? <Spinner /> : items.length === 0 ? (
        <EmptyState title="No applications found" text="Add an application manually, or use 'Add to tracker' on any job." action={<button className="btn-primary" onClick={() => setEditing({ data: blank() })}>Add application</button>} />
      ) : view === 'table' ? (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-slate-100 text-xs uppercase text-slate-500"><tr><th className="p-3">Job</th><th className="p-3">Company</th><th className="p-3">Applied</th><th className="p-3">Status</th><th className="p-3">Interview</th><th className="p-3">Updated</th><th className="p-3" /></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((a) => (
                <tr key={a.id}>
                  <td className="p-3 font-medium">{a.jobTitle}</td><td className="p-3">{a.company}</td><td className="p-3">{formatDate(a.applicationDate)}</td>
                  <td className="p-3"><select aria-label={`Status for ${a.jobTitle}`} className={`rounded-full border-0 px-2 py-1 text-xs font-medium ${statusMeta(a.status).color}`} value={a.status} onChange={(e) => changeStatus(a, e.target.value as ApplicationStatus)}>{APPLICATION_STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}</select></td>
                  <td className="p-3">{formatDateTime(a.interviewDate)}</td><td className="p-3 text-slate-500">{formatDate(a.updatedAt)}</td>
                  <td className="p-3"><div className="flex justify-end gap-1">
                    {a.applicationUrl && <a className="btn-ghost btn-sm" href={a.applicationUrl} target="_blank" rel="noopener noreferrer" aria-label="Open application link"><ExternalLink className="h-4 w-4" /></a>}
                    <button className="btn-ghost btn-sm" onClick={() => showHistory(a)} aria-label="History"><History className="h-4 w-4" /></button>
                    <button className="btn-ghost btn-sm" onClick={() => edit(a)} aria-label="Edit"><Pencil className="h-4 w-4" /></button>
                    <button className="btn-ghost btn-sm text-red-600" onClick={() => setToDelete(a)} aria-label="Delete"><Trash2 className="h-4 w-4" /></button>
                  </div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="flex gap-4 overflow-x-auto pb-4" aria-label="Kanban board">
          {APPLICATION_STATUSES.map((s) => (
            <section key={s.value} className="w-64 shrink-0 rounded-2xl bg-slate-100 p-2" aria-label={s.label}
              onDragOver={(e) => e.preventDefault()}
              onDrop={() => { const a = items.find((x) => x.id === dragId); if (a) void changeStatus(a, s.value); setDragId(null); }}>
              <h2 className="flex items-center justify-between px-2 py-1.5 text-xs font-bold uppercase text-slate-600">{s.label}<span className="rounded-full bg-white px-2 py-0.5 text-slate-500">{grouped.get(s.value)?.length ?? 0}</span></h2>
              <div className="min-h-[3rem] space-y-2">
                {grouped.get(s.value)?.map((a) => (
                  <article key={a.id} draggable onDragStart={() => setDragId(a.id)} className="cursor-grab rounded-xl border border-slate-200 bg-white p-3 text-sm shadow-sm">
                    <p className="font-semibold leading-tight">{a.jobTitle}</p><p className="text-xs text-slate-500">{a.company}</p>
                    {a.interviewDate && <p className="mt-1 text-xs text-amber-700">Interview {formatDateTime(a.interviewDate)}</p>}
                    <select aria-label={`Move ${a.jobTitle}`} className="mt-2 w-full rounded-lg border border-slate-200 px-1 py-1 text-xs" value={a.status} onChange={(e) => changeStatus(a, e.target.value as ApplicationStatus)}>{APPLICATION_STATUSES.map((x) => <option key={x.value} value={x.value}>{x.label}</option>)}</select>
                    <div className="mt-2 flex justify-end gap-1">
                      <button className="btn-ghost btn-sm" onClick={() => showHistory(a)} aria-label="History"><History className="h-3.5 w-3.5" /></button>
                      <button className="btn-ghost btn-sm" onClick={() => edit(a)} aria-label="Edit"><Pencil className="h-3.5 w-3.5" /></button>
                      <button className="btn-ghost btn-sm text-red-600" onClick={() => setToDelete(a)} aria-label="Delete"><Trash2 className="h-3.5 w-3.5" /></button>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}

      {editing && (
        <Modal title={editing.id ? 'Edit application' : 'Add application'} onClose={() => setEditing(null)}>
          <form onSubmit={save} className="space-y-3" noValidate>
            {formError && <Alert>{formError}</Alert>}
            <div className="grid gap-3 sm:grid-cols-2">
              <div><label className="label" htmlFor="a-title">Job title</label><input id="a-title" className="input" value={editing.data.jobTitle ?? ''} onChange={setField('jobTitle')} /></div>
              <div><label className="label" htmlFor="a-company">Company</label><input id="a-company" className="input" value={editing.data.company ?? ''} onChange={setField('company')} /></div>
              <div><label className="label" htmlFor="a-status">Status</label><select id="a-status" className="input" value={editing.data.status} onChange={setField('status')}>{APPLICATION_STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}</select></div>
              <div><label className="label" htmlFor="a-date">Application date</label><input id="a-date" type="date" className="input" value={editing.data.applicationDate ?? ''} onChange={setField('applicationDate')} /></div>
              <div><label className="label" htmlFor="a-contact">Contact person</label><input id="a-contact" className="input" value={editing.data.contactPerson ?? ''} onChange={setField('contactPerson')} /></div>
              <div><label className="label" htmlFor="a-int">Interview date</label><input id="a-int" type="datetime-local" className="input" value={editing.data.interviewDate ?? ''} onChange={setField('interviewDate')} /></div>
            </div>
            <div><label className="label" htmlFor="a-url">Application URL</label><input id="a-url" className="input" value={editing.data.applicationUrl ?? ''} onChange={setField('applicationUrl')} /></div>
            <div><label className="label" htmlFor="a-notes">Notes</label><textarea id="a-notes" rows={3} className="input" value={editing.data.notes ?? ''} onChange={setField('notes')} /></div>
            <div className="flex justify-end gap-2"><button type="button" className="btn-secondary" onClick={() => setEditing(null)}>Cancel</button><button className="btn-primary">Save</button></div>
          </form>
        </Modal>
      )}
      {history && (
        <Modal title={`History: ${history.app.jobTitle}`} onClose={() => setHistory(null)}>
          <ul className="space-y-2 text-sm">
            {history.rows.map((h) => (
              <li key={h.id} className="rounded-xl bg-slate-50 p-3">
                <div className="flex flex-wrap items-center gap-2">{h.fromStatus && <><span className={`badge ${statusMeta(h.fromStatus).color}`}>{statusMeta(h.fromStatus).label}</span><span aria-hidden>→</span></>}<span className={`badge ${statusMeta(h.toStatus).color}`}>{statusMeta(h.toStatus).label}</span></div>
                <p className="mt-1 text-xs text-slate-500">{formatDateTime(h.changedAt)}</p>{h.note && <p className="mt-1 text-slate-700">{h.note}</p>}
              </li>
            ))}
          </ul>
        </Modal>
      )}
      {toDelete && (
        <Modal title="Delete application?" onClose={() => setToDelete(null)}>
          <p className="text-sm text-slate-600">Remove "{toDelete.jobTitle}" at {toDelete.company} and its history?</p>
          <div className="mt-4 flex justify-end gap-2"><button className="btn-secondary" onClick={() => setToDelete(null)}>Cancel</button><button className="btn-danger" onClick={remove}>Delete</button></div>
        </Modal>
      )}
    </div>
  );
}
