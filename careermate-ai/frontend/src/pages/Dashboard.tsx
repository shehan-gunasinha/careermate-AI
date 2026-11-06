import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Bookmark, Bot, Briefcase, CalendarClock, ClipboardList, FileSearch, Gift, Lightbulb, Search, Upload, XCircle } from 'lucide-react';
import { dashboardApi } from '../api';
import { errorMessage } from '../api/client';
import type { Dashboard as DashboardData } from '../api/types';
import JobCard from '../components/JobCard';
import { Alert, EmptyState, PageHeader, ProgressBar, ScoreRing, Spinner, StatCard } from '../components/ui';
import { formatDateTime, statusMeta, timeAgo } from '../lib/format';

const QUICK = [
  { to: '/jobs', label: 'Find Jobs', icon: Search },
  { to: '/resumes', label: 'Upload Resume', icon: Upload },
  { to: '/resumes', label: 'Analyze Resume', icon: FileSearch },
  { to: '/applications', label: 'Track Applications', icon: ClipboardList },
  { to: '/assistant', label: 'Ask CareerMate AI', icon: Bot },
];

export default function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dashboardApi.get().then(setData).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (!data) return <Spinner label="Loading your dashboard..." />;

  const chart = Object.entries(data.statusBreakdown).map(([k, v]) => ({ name: statusMeta(k).label, count: v }));

  return (
    <div className="space-y-6">
      <PageHeader title={`Welcome back, ${data.fullName.split(' ')[0]}`} subtitle="Here is where your job search stands today." />

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Saved jobs" value={data.savedJobs} icon={<Bookmark className="h-5 w-5" />} />
        <StatCard label="Applications" value={data.applications} icon={<Briefcase className="h-5 w-5" />} tone="bg-brand-50 text-brand-700" />
        <StatCard label="Interviews" value={data.interviews} icon={<CalendarClock className="h-5 w-5" />} tone="bg-accent-50 text-accent-700" />
        <StatCard label="Rejected" value={data.rejected} icon={<XCircle className="h-5 w-5" />} tone="bg-red-50 text-red-600" />
      </div>

      <div className="flex flex-wrap gap-2">
        {QUICK.map(({ to, label, icon: Icon }) => (
          <Link key={label} to={to} className="btn-secondary"><Icon className="h-4 w-4" aria-hidden />{label}</Link>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <section className="card p-5 lg:col-span-2" aria-label="Application pipeline">
          <h2 className="mb-4 font-semibold">Application pipeline</h2>
          {data.applications === 0 ? (
            <EmptyState title="No applications yet" text="Add your first application to see your pipeline here." action={<Link to="/applications" className="btn-primary btn-sm">Track an application</Link>} />
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chart} margin={{ left: -20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} interval={0} angle={-20} textAnchor="end" height={60} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#14b8a6" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="card space-y-4 p-5" aria-label="Profile completion">
          <h2 className="font-semibold">Profile completion</h2>
          <div className="text-3xl font-bold">{data.profileCompletion}%</div>
          <ProgressBar value={data.profileCompletion} />
          <Link to="/profile" className="btn-secondary btn-sm">Complete profile</Link>
          <div className="border-t border-slate-100 pt-4">
            <h3 className="mb-2 text-sm font-semibold">Resume analysis</h3>
            {data.resumeSummary ? (
              <div className="flex items-center gap-3">
                <ScoreRing value={data.resumeSummary.overallScore} size={64} label="Resume score" />
                <div className="min-w-0 text-sm">
                  <p className="truncate font-medium">{data.resumeSummary.resumeTitle}</p>
                  <Link to={`/resumes/${data.resumeSummary.resumeId}/analysis`} className="text-brand-600">View full analysis</Link>
                </div>
              </div>
            ) : <p className="text-sm text-slate-500">No analysis yet. <Link to="/resumes" className="text-brand-600">Analyze your resume</Link></p>}
          </div>
        </section>
      </div>

      <section className="card p-5" aria-label="Career suggestions">
        <h2 className="mb-3 flex items-center gap-2 font-semibold"><Lightbulb className="h-4 w-4 text-amber-500" aria-hidden />Career improvement suggestions</h2>
        <ul className="grid gap-2 sm:grid-cols-2">
          {data.suggestions.map((s) => <li key={s} className="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-700">{s}</li>)}
        </ul>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section aria-label="Recommended jobs">
          <div className="mb-3 flex items-center justify-between"><h2 className="font-semibold">Recommended for you</h2><Link to="/recommended" className="text-sm text-brand-600">See all</Link></div>
          <div className="space-y-3">{data.recommendedJobs.length ? data.recommendedJobs.slice(0, 3).map((j) => <JobCard key={j.id} job={j} compact />) : <EmptyState title="No jobs yet" />}</div>
        </section>
        <section aria-label="Recently added jobs">
          <div className="mb-3 flex items-center justify-between"><h2 className="font-semibold">Recently added jobs</h2><Link to="/jobs" className="text-sm text-brand-600">Browse all</Link></div>
          <div className="space-y-3">{data.recentJobs.length ? data.recentJobs.slice(0, 3).map((j) => <JobCard key={j.id} job={j} compact />) : <EmptyState title="No jobs yet" />}</div>
        </section>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card p-5" aria-label="Recent activity">
          <h2 className="mb-3 font-semibold">Recent application activity</h2>
          {data.recentActivities.length === 0 ? <p className="text-sm text-slate-500">No activity yet.</p> : (
            <ul className="space-y-3">
              {data.recentActivities.map((a, i) => (
                <li key={i} className="flex items-start gap-3 text-sm">
                  <span className={`badge ${statusMeta(a.toStatus).color}`}>{statusMeta(a.toStatus).label}</span>
                  <div className="min-w-0 flex-1"><p className="truncate font-medium">{a.jobTitle} <span className="font-normal text-slate-500">at {a.company}</span></p></div>
                  <span className="shrink-0 text-xs text-slate-400">{timeAgo(a.at)}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
        <section className="card p-5" aria-label="Upcoming interviews">
          <h2 className="mb-3 flex items-center gap-2 font-semibold"><Gift className="h-4 w-4 text-accent-600" aria-hidden />Upcoming interviews</h2>
          {data.upcomingInterviews.length === 0 ? <p className="text-sm text-slate-500">No interviews scheduled.</p> : (
            <ul className="space-y-3">
              {data.upcomingInterviews.map((i) => (
                <li key={i.applicationId} className="rounded-xl bg-amber-50 px-3 py-2 text-sm"><p className="font-medium">{i.jobTitle} at {i.company}</p><p className="text-xs text-amber-800">{formatDateTime(i.interviewDate)}</p></li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
