import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Sparkles } from 'lucide-react';
import { jobsApi } from '../api';
import { errorMessage } from '../api/client';
import type { Job, PageResponse } from '../api/types';
import JobCard from '../components/JobCard';
import { Alert, EmptyState, PageHeader, Pagination, Spinner } from '../components/ui';

export default function Recommended() {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PageResponse<Job> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    jobsApi.recommended(page, 10).then(setResult).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, [page]);

  const toggleSave = async (job: Job) => {
    try {
      if (job.saved) await jobsApi.unsave(job.id);
      else await jobsApi.save(job.id);
      setResult((r) => r && { ...r, content: r.content.map((j) => (j.id === job.id ? { ...j, saved: !j.saved } : j)) });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <div>
      <PageHeader title="Recommended Jobs" subtitle="Ranked by how well each job matches your skills, experience, education and preferences."
        actions={<Link to="/profile" className="btn-secondary btn-sm">Improve my matches</Link>} />
      {error && <div className="mb-4"><Alert>{error}</Alert></div>}
      {loading ? <Spinner label="Calculating your best matches..." /> : !result || result.content.length === 0 ? (
        <EmptyState icon={<Sparkles className="h-10 w-10" />} title="No jobs to recommend yet" text="Add skills to your profile and upload a resume so we can rank jobs for you." />
      ) : (
        <>
          <div className="space-y-3">{result.content.map((j) => <JobCard key={j.id} job={j} onToggleSave={toggleSave} />)}</div>
          <Pagination page={result.page} totalPages={result.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}
