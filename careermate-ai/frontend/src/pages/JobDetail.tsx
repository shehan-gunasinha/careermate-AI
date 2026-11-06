import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Bookmark, BookmarkCheck, Building2, CheckCircle2, ClipboardList, ExternalLink, MapPin, Sparkles, TriangleAlert } from 'lucide-react';
import { applicationApi, jobsApi } from '../api';
import { errorMessage } from '../api/client';
import type { Job, MatchResult } from '../api/types';
import { Alert, Badge, ProgressBar, ScoreRing, Spinner } from '../components/ui';
import { EMPLOYMENT_TYPES, EXPERIENCE_LEVELS, WORK_ARRANGEMENTS, formatDate, formatSalary, label } from '../lib/format';

function Block({ title, text }: { title: string; text?: string | null }) {
  if (!text) return null;
  const lines = text.split('\n').map((l) => l.trim()).filter(Boolean);
  return (
    <section className="mt-6">
      <h2 className="mb-2 font-semibold">{title}</h2>
      {lines.length > 1 ? <ul className="ml-5 list-disc space-y-1 text-sm text-slate-700">{lines.map((l, i) => <li key={i}>{l}</li>)}</ul> : <p className="text-sm text-slate-700">{text}</p>}
    </section>
  );
}

export default function JobDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [match, setMatch] = useState<MatchResult | null>(null);
  const [matchError, setMatchError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const jobId = Number(id);

  useEffect(() => {
    setJob(null);
    setMatch(null);
    jobsApi.get(jobId).then(setJob).catch((e) => setError(errorMessage(e, 'Job not found')));
    jobsApi.match(jobId).then(setMatch).catch((e) => setMatchError(errorMessage(e)));
  }, [jobId]);

  if (error) return <div className="space-y-3"><Alert>{error}</Alert><Link to="/jobs" className="btn-secondary btn-sm">Back to jobs</Link></div>;
  if (!job) return <Spinner label="Loading job..." />;

  const salary = formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency);

  const toggleSave = async () => {
    try {
      if (job.saved) await jobsApi.unsave(job.id);
      else await jobsApi.save(job.id);
      setJob({ ...job, saved: !job.saved });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const track = async () => {
    try {
      await applicationApi.create({ jobId: job.id, status: 'SAVED' });
      setNotice('Added to your Application Tracker.');
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <button onClick={() => navigate(-1)} className="btn-ghost btn-sm mb-4"><ArrowLeft className="h-4 w-4" />Back</button>
      {job.demo && <div className="mb-4"><Alert kind="info">This is a <strong>sample demo listing</strong>, not a real vacancy.</Alert></div>}
      {notice && <div className="mb-4"><Alert kind="success">{notice} <Link className="font-semibold underline" to="/applications">Open tracker</Link></Alert></div>}

      <div className="grid gap-6 lg:grid-cols-3">
        <article className="card p-6 lg:col-span-2">
          <div className="flex items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-navy-50 text-navy-700">
              {job.companyLogoUrl ? <img src={job.companyLogoUrl} alt="" className="h-full w-full object-contain" referrerPolicy="no-referrer" /> : <Building2 className="h-7 w-7" aria-hidden />}
            </div>
            <div>
              <h1 className="text-2xl font-bold">{job.title}</h1>
              <p className="text-slate-600">{job.companyName}</p>
              <p className="mt-1 flex items-center gap-1 text-sm text-slate-500"><MapPin className="h-4 w-4" aria-hidden />{job.location}</p>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-1.5">
            <Badge className="bg-navy-50 text-navy-700">{label(EMPLOYMENT_TYPES, job.employmentType)}</Badge>
            <Badge className="bg-accent-50 text-accent-700">{label(WORK_ARRANGEMENTS, job.workArrangement)}</Badge>
            <Badge className="bg-brand-50 text-brand-700">{label(EXPERIENCE_LEVELS, job.experienceLevel)}</Badge>
            {job.industry && <Badge>{job.industry}</Badge>}
            {salary && <Badge className="bg-emerald-50 text-emerald-700">{salary}</Badge>}
          </div>
          <dl className="mt-4 grid gap-2 text-sm text-slate-600 sm:grid-cols-3">
            <div><dt className="text-xs uppercase text-slate-400">Posted</dt><dd>{formatDate(job.postedDate) || 'N/A'}</dd></div>
            <div><dt className="text-xs uppercase text-slate-400">Deadline</dt><dd>{formatDate(job.deadline) || 'Open'}</dd></div>
            <div><dt className="text-xs uppercase text-slate-400">Source</dt><dd>{job.source}</dd></div>
          </dl>
          <div className="mt-5 flex flex-wrap gap-2">
            {job.applicationUrl ? (
              <a className="btn-accent" href={job.applicationUrl} target="_blank" rel="noopener noreferrer">Apply now <ExternalLink className="h-4 w-4" aria-hidden /></a>
            ) : <span className="btn-secondary cursor-default opacity-70" aria-disabled>No application link provided</span>}
            <button className="btn-secondary" onClick={toggleSave} aria-pressed={job.saved}>
              {job.saved ? <BookmarkCheck className="h-4 w-4 text-accent-600" /> : <Bookmark className="h-4 w-4" />}{job.saved ? 'Saved' : 'Save job'}
            </button>
            <button className="btn-secondary" onClick={track}><ClipboardList className="h-4 w-4" />Add to tracker</button>
          </div>

          <section className="mt-6"><h2 className="mb-2 font-semibold">Job description</h2><p className="whitespace-pre-line text-sm text-slate-700">{job.description}</p></section>
          <Block title="Responsibilities" text={job.responsibilities} />
          <Block title="Required qualifications" text={job.requiredQualifications} />
          <Block title="Preferred qualifications" text={job.preferredQualifications} />
          <section className="mt-6">
            <h2 className="mb-2 font-semibold">Skills</h2>
            <div className="flex flex-wrap gap-1.5">
              {job.requiredSkills.map((s) => <Badge key={s} className="bg-navy-50 text-navy-700">{s}</Badge>)}
              {job.preferredSkills.map((s) => <Badge key={s} className="bg-slate-100 text-slate-600">{s} (preferred)</Badge>)}
            </div>
          </section>
          <section className="mt-6"><h2 className="mb-2 font-semibold">About the company</h2><p className="text-sm text-slate-700">{job.companyDescription || `${job.companyName} has not provided a company description.`}</p></section>
          <section className="mt-6"><h2 className="mb-2 font-semibold">How to apply</h2><p className="text-sm text-slate-700">{job.applicationUrl ? 'Use the Apply button to go to the official application page, then update your Application Tracker.' : 'Contact the company directly using the details in the description.'}</p></section>
        </article>

        <aside className="space-y-4" aria-label="AI compatibility">
          <div className="card p-5">
            <h2 className="mb-3 flex items-center gap-2 font-semibold"><Sparkles className="h-4 w-4 text-brand-600" aria-hidden />AI compatibility</h2>
            {matchError && <Alert>{matchError}</Alert>}
            {!match && !matchError && <Spinner label="Analysing your fit..." />}
            {match && (
              <div className="space-y-4">
                <div className="flex items-center gap-4"><ScoreRing value={match.score} label="Compatibility" /><div className="text-sm text-slate-600">Overall compatibility{match.aiPowered ? ' (AI-enhanced)' : ' (algorithmic)'}</div></div>
                <div className="space-y-2 text-xs">
                  {[['Skills', match.skillScore], ['Experience', match.experienceScore], ['Education', match.educationScore], ['Preferences', match.preferenceScore]].map(([n, v]) => (
                    <div key={n as string}><div className="mb-1 flex justify-between"><span>{n}</span><span>{v}%</span></div><ProgressBar value={v as number} /></div>
                  ))}
                </div>
                {match.matchingSkills.length > 0 && <div><h3 className="mb-1 flex items-center gap-1 text-sm font-semibold"><CheckCircle2 className="h-4 w-4 text-emerald-600" />Matching skills</h3><div className="flex flex-wrap gap-1">{match.matchingSkills.map((s) => <Badge key={s} className="bg-emerald-50 text-emerald-700">{s}</Badge>)}</div></div>}
                {match.missingSkills.length > 0 && <div><h3 className="mb-1 flex items-center gap-1 text-sm font-semibold"><TriangleAlert className="h-4 w-4 text-amber-600" />Missing skills</h3><div className="flex flex-wrap gap-1">{match.missingSkills.map((s) => <Badge key={s} className="bg-amber-50 text-amber-800">{s}</Badge>)}</div></div>}
                <div className="text-xs text-slate-600"><p>{match.experienceNote}</p><p className="mt-1">{match.educationNote}</p></div>
                {match.strengths.length > 0 && <div><h3 className="mb-1 text-sm font-semibold">Strengths</h3><ul className="ml-4 list-disc space-y-1 text-xs text-slate-600">{match.strengths.map((s) => <li key={s}>{s}</li>)}</ul></div>}
                {match.improvements.length > 0 && <div><h3 className="mb-1 text-sm font-semibold">Areas to improve</h3><ul className="ml-4 list-disc space-y-1 text-xs text-slate-600">{match.improvements.map((s) => <li key={s}>{s}</li>)}</ul></div>}
                {match.recommendations.length > 0 && <div><h3 className="mb-1 text-sm font-semibold">Recommendations</h3><ul className="ml-4 list-disc space-y-1 text-xs text-slate-600">{match.recommendations.map((s) => <li key={s}>{s}</li>)}</ul></div>}
              </div>
            )}
          </div>
        </aside>
      </div>
    </div>
  );
}
