import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Search, SlidersHorizontal } from 'lucide-react';
import { jobsApi } from '../api';
import { errorMessage } from '../api/client';
import type { Job, JobFilters, PageResponse } from '../api/types';
import JobCard from '../components/JobCard';
import { Alert, EmptyState, PageHeader, Pagination, Spinner } from '../components/ui';
import { EMPLOYMENT_TYPES, EXPERIENCE_LEVELS, INDUSTRIES, WORK_ARRANGEMENTS } from '../lib/format';

const EMPTY: JobFilters = { q: '', location: '', industry: '', employmentType: '', experienceLevel: '', workArrangement: '', minSalary: '', maxSalary: '' };

export default function Jobs() {
  const [tab, setTab] = useState<'all' | 'saved'>('all');
  const [draft, setDraft] = useState<JobFilters>(EMPTY);
  const [applied, setApplied] = useState<JobFilters>(EMPTY);
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState('postedDate,desc');
  const [result, setResult] = useState<PageResponse<Job> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const req = tab === 'saved' ? jobsApi.saved(page, 12) : jobsApi.search({ ...applied, page, size: 12, sort });
    req.then(setResult).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, [tab, applied, page, sort]);

  useEffect(load, [load]);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    setApplied(draft);
  };

  const reset = () => {
    setDraft(EMPTY);
    setApplied(EMPTY);
    setPage(0);
  };

  const toggleSave = async (job: Job) => {
    setSavingId(job.id);
    try {
      if (job.saved) await jobsApi.unsave(job.id);
      else await jobsApi.save(job.id);
      setResult((r) => {
        if (!r) return r;
        if (tab === 'saved' && job.saved) return { ...r, content: r.content.filter((j) => j.id !== job.id), totalElements: r.totalElements - 1 };
        return { ...r, content: r.content.map((j) => (j.id === job.id ? { ...j, saved: !j.saved } : j)) };
      });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSavingId(null);
    }
  };

  const set = (k: keyof JobFilters) => (e: { target: { value: string } }) => setDraft({ ...draft, [k]: e.target.value });

  return (
    <div>
      <PageHeader title="Find Jobs" subtitle="Search opportunities across every industry." />
      <div className="mb-4 flex gap-2" role="tablist">
        {(['all', 'saved'] as const).map((t) => (
          <button key={t} role="tab" aria-selected={tab === t} className={tab === t ? 'btn-primary btn-sm' : 'btn-secondary btn-sm'} onClick={() => { setTab(t); setPage(0); }}>
            {t === 'all' ? 'All jobs' : 'Saved jobs'}
          </button>
        ))}
      </div>

      {tab === 'all' && (
        <form onSubmit={submit} className="card mb-6 space-y-4 p-4" aria-label="Job search filters">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-slate-400" aria-hidden />
              <input aria-label="Search jobs" className="input pl-9" placeholder="Job title, keywords, skill or company" value={draft.q} onChange={set('q')} />
            </div>
            <button type="submit" className="btn-primary">Search</button>
            <button type="button" className="btn-secondary lg:hidden" onClick={() => setShowFilters(!showFilters)} aria-expanded={showFilters}><SlidersHorizontal className="h-4 w-4" />Filters</button>
          </div>
          <div className={`${showFilters ? 'grid' : 'hidden'} gap-3 sm:grid-cols-2 lg:grid lg:grid-cols-4`}>
            <div><label className="label" htmlFor="f-loc">Location</label><input id="f-loc" className="input" value={draft.location} onChange={set('location')} /></div>
            <div><label className="label" htmlFor="f-ind">Industry</label>
              <select id="f-ind" className="input" value={draft.industry} onChange={set('industry')}><option value="">Any</option>{INDUSTRIES.map((i) => <option key={i}>{i}</option>)}</select></div>
            <div><label className="label" htmlFor="f-type">Employment type</label>
              <select id="f-type" className="input" value={draft.employmentType} onChange={set('employmentType')}><option value="">Any</option>{EMPLOYMENT_TYPES.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select></div>
            <div><label className="label" htmlFor="f-exp">Experience level</label>
              <select id="f-exp" className="input" value={draft.experienceLevel} onChange={set('experienceLevel')}><option value="">Any</option>{EXPERIENCE_LEVELS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select></div>
            <div><label className="label" htmlFor="f-arr">Work arrangement</label>
              <select id="f-arr" className="input" value={draft.workArrangement} onChange={set('workArrangement')}><option value="">Any</option>{WORK_ARRANGEMENTS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select></div>
            <div><label className="label" htmlFor="f-min">Min salary</label><input id="f-min" type="number" min="0" className="input" value={draft.minSalary} onChange={set('minSalary')} /></div>
            <div><label className="label" htmlFor="f-max">Max salary</label><input id="f-max" type="number" min="0" className="input" value={draft.maxSalary} onChange={set('maxSalary')} /></div>
            <div><label className="label" htmlFor="f-sort">Sort by</label>
              <select id="f-sort" className="input" value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
                <option value="postedDate,desc">Newest first</option><option value="postedDate,asc">Oldest first</option>
                <option value="title,asc">Title A-Z</option><option value="salaryMax,desc">Highest salary</option><option value="deadline,asc">Deadline soonest</option>
              </select></div>
          </div>
          <div><button type="button" className="btn-ghost btn-sm" onClick={reset}>Clear filters</button></div>
        </form>
      )}

      {error && <div className="mb-4"><Alert>{error}</Alert></div>}
      {loading ? <Spinner label="Loading jobs..." /> : !result || result.content.length === 0 ? (
        <EmptyState title={tab === 'saved' ? 'No saved jobs yet' : 'No jobs match your search'} text={tab === 'saved' ? 'Save jobs you like and they will appear here.' : 'Try fewer filters or different keywords.'} />
      ) : (
        <>
          <p className="mb-3 text-sm text-slate-500">{result.totalElements} job{result.totalElements === 1 ? '' : 's'} found</p>
          <div className="space-y-3">{result.content.map((j) => <JobCard key={j.id} job={j} onToggleSave={toggleSave} saving={savingId === j.id} />)}</div>
          <Pagination page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
