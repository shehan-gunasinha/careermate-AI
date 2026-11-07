import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Bell, Bot, ClipboardList, FileText, LayoutDashboard, LogOut, Menu, Search, Settings, Sparkles, User as UserIcon,
} from 'lucide-react';
import { notificationApi } from '../api';
import { useAuth } from '../context/AuthContext';
import { ConfirmDialog } from './ui';

const NAV = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/jobs', label: 'Find Jobs', icon: Search },
  { to: '/recommended', label: 'Recommended Jobs', icon: Sparkles },
  { to: '/resumes', label: 'Resume Studio', icon: FileText },
  { to: '/applications', label: 'Application Tracker', icon: ClipboardList },
  { to: '/assistant', label: 'CareerMate AI', icon: Bot },
  { to: '/notifications', label: 'Notifications', icon: Bell },
  { to: '/profile', label: 'My Profile', icon: UserIcon },
  { to: '/settings', label: 'Settings', icon: Settings },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [confirmLogout, setConfirmLogout] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const initials = (user?.fullName ?? '').trim().split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('') || '?';

  useEffect(() => setOpen(false), [location.pathname]);

  useEffect(() => {
    let alive = true;
    const load = () => notificationApi.unreadCount().then((r) => alive && setUnread(r.count)).catch(() => undefined);
    load();
    const t = window.setInterval(load, 60000);
    const onChange = () => load();
    window.addEventListener('notifications:changed', onChange);
    return () => {
      alive = false;
      window.clearInterval(t);
      window.removeEventListener('notifications:changed', onChange);
    };
  }, [location.pathname]);

  const doLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
      navigate('/login');
    } finally {
      setLoggingOut(false);
    }
  };

  const sidebar = (
    <nav className="flex h-full flex-col bg-navy-800 text-navy-100" aria-label="Main navigation">
      <div className="flex items-center gap-2 px-5 py-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent-500 text-white"><Sparkles className="h-5 w-5" aria-hidden /></div>
        <span className="text-lg font-bold text-white">CareerMate <span className="text-accent-500">AI</span></span>
      </div>
      <ul className="flex-1 space-y-1 overflow-y-auto px-3">
        {NAV.map(({ to, label, icon: Icon }) => (
          <li key={to}>
            <NavLink
              to={to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${isActive ? 'bg-white/10 text-white' : 'text-navy-200 hover:bg-white/5 hover:text-white'}`}
            >
              <Icon className="h-4.5 w-4.5 h-[18px] w-[18px]" aria-hidden />
              <span className="flex-1">{label}</span>
              {to === '/notifications' && unread > 0 && (
                <span className="rounded-full bg-accent-500 px-2 py-0.5 text-xs font-semibold text-white" aria-label={`${unread} unread`}>{unread}</span>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
      <div className="border-t border-white/10 p-3">
        <div className="mb-2 flex items-center gap-3 px-3 text-xs text-navy-200">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="h-9 w-9 shrink-0 rounded-full object-cover ring-1 ring-white/20" />
          ) : (
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-white/10 text-sm font-semibold text-white" aria-hidden>{initials}</div>
          )}
          <div className="min-w-0">
            <div className="truncate font-semibold text-white">{user?.fullName}</div>
            <div className="truncate">{user?.email}</div>
          </div>
        </div>
        <button onClick={() => setConfirmLogout(true)} className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-navy-200 hover:bg-white/5 hover:text-white">
          <LogOut className="h-[18px] w-[18px]" aria-hidden /> Logout
        </button>
      </div>
    </nav>
  );

  return (
    <div className="flex min-h-screen">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 lg:block">{sidebar}</aside>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/50" onClick={() => setOpen(false)} aria-hidden />
          <div className="absolute inset-y-0 left-0 w-72 max-w-[85%]">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col lg:pl-64">
        <header className="sticky top-0 z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur lg:hidden">
          <button className="btn-ghost btn-sm" onClick={() => setOpen(true)} aria-label="Open menu"><Menu className="h-5 w-5" /></button>
          <span className="font-bold text-navy-800">CareerMate <span className="text-accent-600">AI</span></span>
          <button className="btn-ghost btn-sm relative" onClick={() => navigate('/notifications')} aria-label="Notifications">
            <Bell className="h-5 w-5" />
            {unread > 0 && <span className="absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full bg-accent-500" />}
          </button>
        </header>
        <main className="mx-auto w-full max-w-7xl flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>

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
