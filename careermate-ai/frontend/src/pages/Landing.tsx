import { Link } from 'react-router-dom';
import { Bot, ClipboardList, FileText, Search, Sparkles, Target } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const FEATURES = [
  { icon: Search, title: 'Discover jobs', text: 'Search by title, company, industry, location, salary and work arrangement across every profession.' },
  { icon: Target, title: 'AI job matching', text: 'See a compatibility score with matching skills, gaps and practical recommendations for each job.' },
  { icon: FileText, title: 'Resume Studio', text: 'Upload a PDF or DOCX for scoring, ATS checks and improvement tips, or build a polished PDF resume.' },
  { icon: ClipboardList, title: 'Track applications', text: 'Keep every application, interview and status change organised in a table or Kanban board.' },
  { icon: Bot, title: 'CareerMate AI assistant', text: 'Interview prep, salary answers, cover emails and career plans, personalised from your profile.' },
  { icon: Sparkles, title: 'Smart reminders', text: 'Deadline, interview and resume reminders so nothing slips through.' },
];

const INDUSTRIES = ['Information Technology', 'Engineering', 'Finance & Accounting', 'Marketing', 'Business Management', 'Healthcare', 'Education', 'Administration', 'Hospitality', 'Customer Service'];

export default function Landing() {
  const { user } = useAuth();
  return (
    <div className="min-h-screen bg-white">
      <header className="border-b border-slate-100">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
          <span className="flex items-center gap-2 text-lg font-bold text-navy-800">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-navy-700 text-white"><Sparkles className="h-5 w-5" /></span>
            CareerMate <span className="text-accent-600">AI</span>
          </span>
          <nav className="flex items-center gap-2">
            {user ? <Link to="/dashboard" className="btn-primary">Go to dashboard</Link> : (
              <>
                <Link to="/login" className="btn-ghost">Log in</Link>
                <Link to="/register" className="btn-primary">Get started</Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <section className="bg-gradient-to-br from-navy-800 via-navy-700 to-brand-700 text-white">
        <div className="mx-auto max-w-6xl px-4 py-20 text-center sm:py-28">
          <p className="mx-auto mb-4 inline-block rounded-full bg-white/10 px-3 py-1 text-xs font-medium text-accent-100">AI-powered career development</p>
          <h1 className="mx-auto max-w-3xl text-4xl font-extrabold tracking-tight text-white sm:text-5xl">Find the right job. Build a stronger resume. Get hired.</h1>
          <p className="mx-auto mt-5 max-w-2xl text-lg text-navy-100">CareerMate AI brings job discovery, resume analysis, application tracking and an AI career coach into one place, for every industry.</p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link to={user ? '/jobs' : '/register'} className="btn-accent px-6 py-3 text-base">{user ? 'Find jobs' : 'Create free account'}</Link>
            {!user && <Link to="/login" className="btn border border-white/30 px-6 py-3 text-base text-white hover:bg-white/10">I already have an account</Link>}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16">
        <h2 className="text-center text-3xl font-bold">Everything you need in one platform</h2>
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, text }) => (
            <div key={title} className="card p-5">
              <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-accent-50 text-accent-700"><Icon className="h-5 w-5" aria-hidden /></div>
              <h3 className="font-semibold">{title}</h3>
              <p className="mt-1 text-sm text-slate-600">{text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-slate-50 py-14">
        <div className="mx-auto max-w-6xl px-4 text-center">
          <h2 className="text-2xl font-bold">Built for every profession</h2>
          <div className="mt-6 flex flex-wrap justify-center gap-2">
            {INDUSTRIES.map((i) => <span key={i} className="rounded-full border border-slate-200 bg-white px-4 py-1.5 text-sm text-slate-700">{i}</span>)}
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-100 py-8 text-center text-sm text-slate-500">
        <p>CareerMate AI. Sample job listings in demo mode are fictional and are not real vacancies.</p>
      </footer>
    </div>
  );
}
