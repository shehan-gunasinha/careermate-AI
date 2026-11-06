import { Bookmark, BookmarkCheck, Building2, Calendar, ExternalLink, MapPin, Wallet } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { Job } from '../api/types';
import { EMPLOYMENT_TYPES, EXPERIENCE_LEVELS, WORK_ARRANGEMENTS, formatDate, formatSalary, label } from '../lib/format';
import { Badge, ScoreRing } from './ui';

interface Props {
  job: Job;
  onToggleSave?: (job: Job) => void;
  saving?: boolean;
  compact?: boolean;
}

export default function JobCard({ job, onToggleSave, saving, compact }: Props) {
  const salary = formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency);
  return (
    <article className="card flex flex-col gap-3 p-4 transition hover:border-accent-500/50 sm:flex-row" aria-label={job.title}>
      <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-navy-50 text-navy-700">
        {job.companyLogoUrl ? <img src={job.companyLogoUrl} alt="" className="h-full w-full object-contain" referrerPolicy="no-referrer" /> : <Building2 className="h-6 w-6" aria-hidden />}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="min-w-0">
            <Link to={`/jobs/${job.id}`} className="block truncate text-base font-semibold text-navy-800 hover:text-brand-600">{job.title}</Link>
            <p className="text-sm text-slate-600">{job.companyName}</p>
          </div>
          {job.matchScore != null && <ScoreRing value={job.matchScore} size={56} label="Match" />}
        </div>
        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
          <span className="inline-flex items-center gap-1"><MapPin className="h-3.5 w-3.5" aria-hidden />{job.location}</span>
          {salary && <span className="inline-flex items-center gap-1"><Wallet className="h-3.5 w-3.5" aria-hidden />{salary}</span>}
          {job.postedDate && <span className="inline-flex items-center gap-1"><Calendar className="h-3.5 w-3.5" aria-hidden />Posted {formatDate(job.postedDate)}</span>}
          {job.deadline && <span className="text-amber-700">Apply by {formatDate(job.deadline)}</span>}
        </div>
        <div className="mt-2 flex flex-wrap gap-1.5">
          <Badge className="bg-navy-50 text-navy-700">{label(EMPLOYMENT_TYPES, job.employmentType)}</Badge>
          <Badge className="bg-accent-50 text-accent-700">{label(WORK_ARRANGEMENTS, job.workArrangement)}</Badge>
          <Badge className="bg-brand-50 text-brand-700">{label(EXPERIENCE_LEVELS, job.experienceLevel)}</Badge>
          {job.demo && <Badge className="bg-amber-100 text-amber-800">Demo listing</Badge>}
          <Badge className="bg-slate-100 text-slate-500">Source: {job.source}</Badge>
        </div>
        {!compact && job.shortDescription && <p className="mt-2 line-clamp-2 text-sm text-slate-600">{job.shortDescription}</p>}
        {!compact && job.requiredSkills.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {job.requiredSkills.slice(0, 6).map((s) => <span key={s} className="rounded-md bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{s}</span>)}
          </div>
        )}
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <Link to={`/jobs/${job.id}`} className="btn-primary btn-sm">View details</Link>
          {job.applicationUrl && (
            <a href={job.applicationUrl} target="_blank" rel="noopener noreferrer" className="btn-secondary btn-sm">
              Apply <ExternalLink className="h-3.5 w-3.5" aria-hidden />
            </a>
          )}
          {onToggleSave && (
            <button className="btn-ghost btn-sm" onClick={() => onToggleSave(job)} disabled={saving} aria-pressed={job.saved}>
              {job.saved ? <BookmarkCheck className="h-4 w-4 text-accent-600" aria-hidden /> : <Bookmark className="h-4 w-4" aria-hidden />}
              {job.saved ? 'Saved' : 'Save'}
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
