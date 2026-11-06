import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Download, Eye, FileSearch, FileText, PenLine, Trash2, Upload } from 'lucide-react';
import { resumeApi } from '../api';
import { errorMessage } from '../api/client';
import type { Resume } from '../api/types';
import { Alert, Badge, EmptyState, Modal, PageHeader, Spinner } from '../components/ui';
import { formatDateTime, scoreColor } from '../lib/format';

export default function Resumes() {
  const navigate = useNavigate();
  const [resumes, setResumes] = useState<Resume[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [viewing, setViewing] = useState<Resume | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<Resume | null>(null);
  const input = useRef<HTMLInputElement>(null);

  const load = useCallback(() => {
    resumeApi.list().then(setResumes).catch((e) => setError(errorMessage(e)));
  }, []);
  useEffect(load, [load]);

  const upload = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setNotice(null);
    if (!file) return setError('Choose a PDF or DOCX file first.');
    const ext = file.name.split('.').pop()?.toLowerCase();
    if (ext !== 'pdf' && ext !== 'docx') return setError('Only PDF and DOCX files are supported.');
    if (file.size > 5 * 1024 * 1024) return setError('File is too large. The limit is 5 MB.');
    setBusy('upload');
    try {
      await resumeApi.upload(file, title.trim() || undefined);
      setNotice('Resume uploaded and text extracted.');
      setFile(null);
      setTitle('');
      if (input.current) input.current.value = '';
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(null);
    }
  };

  const analyze = async (r: Resume) => {
    setBusy(`analyze-${r.id}`);
    setError(null);
    try {
      await resumeApi.analyze(r.id);
      navigate(`/resumes/${r.id}/analysis`);
    } catch (err) {
      setError(errorMessage(err));
      setBusy(null);
    }
  };

  const view = async (r: Resume) => {
    try {
      setViewing(await resumeApi.get(r.id));
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const remove = async () => {
    if (!confirmDelete) return;
    try {
      await resumeApi.remove(confirmDelete.id);
      setConfirmDelete(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <div>
      <PageHeader title="Resume Studio" subtitle="Upload, analyse and build resumes."
        actions={<Link to="/resume-builder" className="btn-accent"><PenLine className="h-4 w-4" />Build a resume</Link>} />
      {error && <div className="mb-4"><Alert>{error}</Alert></div>}
      {notice && <div className="mb-4"><Alert kind="success">{notice}</Alert></div>}

      <form onSubmit={upload} className="card mb-6 grid gap-4 p-5 md:grid-cols-[1fr_1fr_auto] md:items-end" aria-label="Upload resume">
        <div>
          <label htmlFor="resume-file" className="label">Resume file (PDF or DOCX, max 5 MB)</label>
          <input id="resume-file" ref={input} type="file" accept=".pdf,.docx" className="input file:mr-3 file:rounded-lg file:border-0 file:bg-navy-50 file:px-3 file:py-1 file:text-sm file:font-medium file:text-navy-700"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        </div>
        <div>
          <label htmlFor="resume-title" className="label">Title (optional)</label>
          <input id="resume-title" className="input" value={title} maxLength={100} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Software Engineer CV" />
        </div>
        <button className="btn-primary" disabled={busy === 'upload'}><Upload className="h-4 w-4" />{busy === 'upload' ? 'Uploading...' : 'Upload'}</button>
      </form>

      {!resumes ? <Spinner /> : resumes.length === 0 ? (
        <EmptyState icon={<FileText className="h-10 w-10" />} title="No resumes yet" text="Upload an existing resume for analysis or build a new one with the Resume Builder." />
      ) : (
        <ul className="space-y-3">
          {resumes.map((r) => (
            <li key={r.id} className="card flex flex-wrap items-center gap-4 p-4">
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-navy-50 text-navy-700"><FileText className="h-5 w-5" aria-hidden /></div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-semibold">{r.title}</p>
                <p className="text-xs text-slate-500">{r.originalFilename} · {(r.sizeBytes / 1024).toFixed(0)} KB · {formatDateTime(r.createdAt)}</p>
                <div className="mt-1 flex gap-1.5"><Badge className={r.source === 'BUILT' ? 'bg-brand-50 text-brand-700' : 'bg-slate-100 text-slate-600'}>{r.source === 'BUILT' ? 'Built here' : 'Uploaded'}</Badge></div>
              </div>
              {r.latestScore != null && <div className={`text-center ${scoreColor(r.latestScore)}`}><div className="text-2xl font-bold">{r.latestScore}</div><div className="text-[10px] uppercase text-slate-400">score</div></div>}
              <div className="flex flex-wrap gap-2">
                <button className="btn-primary btn-sm" onClick={() => analyze(r)} disabled={busy === `analyze-${r.id}`}><FileSearch className="h-4 w-4" />{busy === `analyze-${r.id}` ? 'Analysing...' : 'Analyze'}</button>
                {r.latestScore != null && <Link to={`/resumes/${r.id}/analysis`} className="btn-secondary btn-sm">Results</Link>}
                <button className="btn-secondary btn-sm" onClick={() => view(r)}><Eye className="h-4 w-4" />Details</button>
                <button className="btn-secondary btn-sm" onClick={() => resumeApi.download(r.id, r.originalFilename).catch((e) => setError(errorMessage(e)))}><Download className="h-4 w-4" />Download</button>
                <button className="btn-ghost btn-sm text-red-600" onClick={() => setConfirmDelete(r)} aria-label={`Delete ${r.title}`}><Trash2 className="h-4 w-4" /></button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {viewing && (
        <Modal title={viewing.title} onClose={() => setViewing(null)} wide>
          {viewing.extractedInfo && (
            <div className="mb-4 grid gap-3 text-sm sm:grid-cols-2">
              <div><span className="text-slate-500">Email:</span> {viewing.extractedInfo.email ?? 'Not found'}</div>
              <div><span className="text-slate-500">Phone:</span> {viewing.extractedInfo.phone ?? 'Not found'}</div>
              <div><span className="text-slate-500">Word count:</span> {viewing.extractedInfo.wordCount}</div>
              <div><span className="text-slate-500">Sections found:</span> {viewing.extractedInfo.detectedSections.join(', ') || 'None detected'}</div>
              <div className="sm:col-span-2"><span className="text-slate-500">Skills detected:</span> <span className="inline-flex flex-wrap gap-1 align-middle">{viewing.extractedInfo.detectedSkills.map((s) => <Badge key={s} className="bg-navy-50 text-navy-700">{s}</Badge>)}</span></div>
            </div>
          )}
          <h3 className="mb-1 text-sm font-semibold">Extracted text</h3>
          <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-xl bg-slate-50 p-3 text-xs text-slate-700">{viewing.extractedText}</pre>
        </Modal>
      )}
      {confirmDelete && (
        <Modal title="Delete resume?" onClose={() => setConfirmDelete(null)}>
          <p className="text-sm text-slate-600">"{confirmDelete.title}" and its analysis history will be permanently deleted.</p>
          <div className="mt-4 flex justify-end gap-2"><button className="btn-secondary" onClick={() => setConfirmDelete(null)}>Cancel</button><button className="btn-danger" onClick={remove}>Delete</button></div>
        </Modal>
      )}
    </div>
  );
}
