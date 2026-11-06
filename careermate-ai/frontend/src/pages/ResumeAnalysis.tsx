import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, FileSearch, Lightbulb, SpellCheck, TriangleAlert } from 'lucide-react';
import { resumeApi } from '../api';
import { errorMessage } from '../api/client';
import type { Resume, ResumeAnalysis as Analysis } from '../api/types';
import { Alert, Badge, ProgressBar, ScoreRing, Spinner } from '../components/ui';
import { formatDateTime } from '../lib/format';

function List({ title, items, icon, tone }: { title: string; items: string[]; icon: React.ReactNode; tone: string }) {
  if (items.length === 0) return null;
  return (
    <section className="card p-5">
      <h2 className={`mb-3 flex items-center gap-2 font-semibold ${tone}`}>{icon}{title}</h2>
      <ul className="ml-5 list-disc space-y-1.5 text-sm text-slate-700">{items.map((s, i) => <li key={i}>{s}</li>)}</ul>
    </section>
  );
}

export default function ResumeAnalysisPage() {
  const id = Number(useParams().id);
  const [resume, setResume] = useState<Resume | null>(null);
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [history, setHistory] = useState<Analysis[]>([]);
  const [loading, setLoading] = useState(true);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setResume(await resumeApi.get(id));
      const h = await resumeApi.history(id);
      setHistory(h);
      setAnalysis(h[0] ?? null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { void load(); }, [load]);

  const run = async () => {
    setRunning(true);
    setError(null);
    try {
      await resumeApi.analyze(id);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setRunning(false);
    }
  };

  if (loading) return <Spinner label="Loading analysis..." />;
  if (!resume) return <div className="space-y-3"><Alert>{error ?? 'Resume not found'}</Alert><Link to="/resumes" className="btn-secondary btn-sm">Back to Resume Studio</Link></div>;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link to="/resumes" className="btn-ghost btn-sm mb-2"><ArrowLeft className="h-4 w-4" />Resume Studio</Link>
          <h1 className="text-2xl font-bold">{resume.title}</h1>
          <p className="text-sm text-slate-500">{resume.originalFilename}</p>
        </div>
        <button className="btn-primary" onClick={run} disabled={running}><FileSearch className="h-4 w-4" />{running ? 'Analysing...' : analysis ? 'Re-run analysis' : 'Analyze resume'}</button>
      </div>
      {error && <Alert>{error}</Alert>}

      {!analysis ? (
        <div className="card p-8 text-center text-slate-600">This resume has not been analysed yet. Run an analysis to see your score and recommendations.</div>
      ) : (
        <>
          <section className="card flex flex-wrap items-center gap-6 p-6">
            <ScoreRing value={analysis.overallScore} size={120} label="Overall resume score" />
            <div className="min-w-0 flex-1">
              <h2 className="text-lg font-semibold">Overall score</h2>
              <p className="mt-1 text-sm text-slate-600">{analysis.summary}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
                <Badge className={analysis.aiPowered ? 'bg-brand-50 text-brand-700' : 'bg-slate-100 text-slate-600'}>{analysis.aiPowered ? 'AI-enhanced analysis' : 'Automated analysis (AI not available)'}</Badge>
                <span>Analysed {formatDateTime(analysis.createdAt)}</span>
              </div>
            </div>
          </section>

          <section className="card p-5" aria-label="Section scores">
            <h2 className="mb-4 font-semibold">Section-wise scores</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              {Object.entries(analysis.sectionScores).map(([name, score]) => (
                <div key={name}><div className="mb-1 flex justify-between text-sm"><span>{name}</span><span className="font-medium">{score}/100</span></div><ProgressBar value={score} /></div>
              ))}
            </div>
          </section>

          <div className="grid gap-6 lg:grid-cols-2">
            <List title="Strengths" items={analysis.strengths} icon={<CheckCircle2 className="h-4 w-4" />} tone="text-emerald-700" />
            <List title="Weaknesses" items={analysis.weaknesses} icon={<TriangleAlert className="h-4 w-4" />} tone="text-amber-700" />
            <List title="Missing information" items={analysis.missingInformation} icon={<TriangleAlert className="h-4 w-4" />} tone="text-red-700" />
            <List title="Recommended improvements" items={analysis.recommendations} icon={<Lightbulb className="h-4 w-4" />} tone="text-brand-700" />
            <List title="Spelling and grammar" items={analysis.languageIssues} icon={<SpellCheck className="h-4 w-4" />} tone="text-slate-700" />
          </div>

          {history.length > 1 && (
            <section className="card p-5" aria-label="Analysis history">
              <h2 className="mb-3 font-semibold">Analysis history</h2>
              <ul className="divide-y divide-slate-100 text-sm">
                {history.map((h) => (
                  <li key={h.id} className="flex items-center justify-between py-2">
                    <span>{formatDateTime(h.createdAt)}</span>
                    <button className="font-semibold text-brand-600" onClick={() => setAnalysis(h)}>Score {h.overallScore}</button>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  );
}
