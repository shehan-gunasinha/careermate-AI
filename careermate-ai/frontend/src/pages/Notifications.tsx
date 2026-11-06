import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { notificationApi } from '../api';
import { errorMessage } from '../api/client';
import type { AppNotification } from '../api/types';
import { Alert, EmptyState, PageHeader, Pagination, Spinner } from '../components/ui';
import { timeAgo } from '../lib/format';

export default function Notifications() {
  const [items, setItems] = useState<AppNotification[] | null>(null);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    notificationApi.list(unreadOnly, page).then((r) => { setItems(r.content); setPages(r.totalPages); }).catch((e) => setError(errorMessage(e)));
  }, [unreadOnly, page]);
  useEffect(load, [load]);

  const changed = () => window.dispatchEvent(new Event('notifications:changed'));

  const markRead = async (n: AppNotification) => {
    if (n.read) return;
    try {
      await notificationApi.read(n.id);
      setItems((l) => l && l.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      changed();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const markAll = async () => {
    try {
      await notificationApi.readAll();
      setItems((l) => l && l.map((x) => ({ ...x, read: true })));
      changed();
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <PageHeader title="Notifications" subtitle="Reminders, recommendations and status updates."
        actions={<>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={unreadOnly} onChange={(e) => { setUnreadOnly(e.target.checked); setPage(0); }} />Unread only</label>
          <button className="btn-secondary btn-sm" onClick={markAll}><CheckCheck className="h-4 w-4" />Mark all as read</button>
        </>} />
      {error && <div className="mb-4"><Alert>{error}</Alert></div>}
      {!items ? <Spinner /> : items.length === 0 ? <EmptyState icon={<Bell className="h-10 w-10" />} title="You're all caught up" text="New reminders and updates will appear here." /> : (
        <>
          <ul className="space-y-2">
            {items.map((n) => (
              <li key={n.id} className={`card flex items-start gap-3 p-4 ${n.read ? '' : 'border-accent-500/40 bg-accent-50/40'}`}>
                <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${n.read ? 'bg-slate-200' : 'bg-accent-500'}`} aria-label={n.read ? 'Read' : 'Unread'} />
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">{n.title}</p><p className="text-sm text-slate-600">{n.message}</p>
                  <div className="mt-1 flex items-center gap-3 text-xs text-slate-400">{timeAgo(n.createdAt)}{n.link && <Link to={n.link} onClick={() => markRead(n)} className="font-medium text-brand-600">Open</Link>}</div>
                </div>
                {!n.read && <button className="btn-ghost btn-sm" onClick={() => markRead(n)}>Mark read</button>}
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={pages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
