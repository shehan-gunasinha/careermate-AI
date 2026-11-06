import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Download, Plus, Sparkles, Trash2, Wand2 } from 'lucide-react';
import { profileApi, resumeApi } from '../api';
import { errorMessage } from '../api/client';
import type { Resume, ResumeBuildInput } from '../api/types';
import { Alert, PageHeader, TagInput } from '../components/ui';

const EMPTY: ResumeBuildInput = {
  title: '', template: 'modern',
  personal: { fullName: '', email: '', phone: '', location: '', headline: '', links: '' },
  summary: '', experience: [], education: [], skills: [], projects: [], certifications: [], achievements: [],
};

function SuggestBox({ section, content, role, onApply }: { section: string; content: string; role?: string; onApply: (t: string) => void }) {
  const [text, setText] = useState<string | null>(null);
  const [ai, setAi] = useState(false);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const run = async () => {
    setBusy(true);
    setErr(null);
    try {
      const r = await resumeApi.suggest(section, content, role);
      setText(r.suggestion);
      setAi(r.aiPowered);
    } catch (e) {
      setErr(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="mt-1">
      <button type="button" className="btn-ghost btn-sm" disabled={busy || content.trim().length < 10} onClick={run}><Wand2 className="h-3.5 w-3.5" />{busy ? 'Thinking...' : 'Suggest improvement'}</button>
      {err && <p className="field-error">{err}</p>}
      {text !== null && (
        <div className="mt-2 rounded-xl border border-brand-200 bg-brand-50 p-3">
          <p className="mb-1 text-xs text-brand-700">{ai ? 'AI suggestion' : 'Offline suggestion'} (editable; check it only contains facts that are true for you)</p>
          <textarea className="input" rows={4} value={text} onChange={(e) => setText(e.target.value)} aria-label="Suggested text" />
          <div className="mt-2 flex gap-2"><button type="button" className="btn-primary btn-sm" onClick={() => { onApply(text); setText(null); }}>Use this text</button><button type="button" className="btn-secondary btn-sm" onClick={() => setText(null)}>Dismiss</button></div>
        </div>
      )}
    </div>
  );
}

function Preview({ r }: { r: ResumeBuildInput }) {
  const modern = r.template === 'modern';
  const h = `mt-4 border-b pb-0.5 text-[11px] font-bold uppercase tracking-wide ${modern ? 'border-accent-600 text-accent-700' : 'border-slate-900 text-slate-900'}`;
  const bullets = (t?: string) => (t ?? '').split('\n').map((l) => l.trim().replace(/^[-*\u2022]\s*/, '')).filter(Boolean);
  const contact = [r.personal.email, r.personal.phone, r.personal.location, r.personal.links].filter(Boolean).join('  |  ');
  return (
    <div className="card bg-white p-6 text-[12px] leading-snug text-slate-800" aria-label="Resume preview">
      <div className={modern ? 'text-left' : 'text-center'}>
        <div className={`text-2xl font-bold ${modern ? 'text-navy-700' : 'text-slate-900'}`}>{r.personal.fullName || 'Your Name'}</div>
        {r.personal.headline && <div className="text-slate-600">{r.personal.headline}</div>}
        {contact && <div className="text-[11px] text-slate-500">{contact}</div>}
      </div>
      {r.summary && <><div className={h}>Professional summary</div><p className="mt-1">{r.summary}</p></>}
      {r.experience.length > 0 && <div className={h}>Work experience</div>}
      {r.experience.map((e, i) => (
        <div key={i} className="mt-1.5"><div className="font-semibold">{e.title}, {e.company} <span className="font-normal text-slate-500">({[e.startDate, e.current ? 'Present' : e.endDate].filter(Boolean).join(' - ')})</span></div>
          <ul className="ml-4 list-disc">{bullets(e.description).map((b, j) => <li key={j}>{b}</li>)}</ul></div>
      ))}
      {r.education.length > 0 && <div className={h}>Education</div>}
      {r.education.map((e, i) => <div key={i} className="mt-1.5 font-semibold">{[e.degree, e.fieldOfStudy].filter(Boolean).join(', ')}{e.degree || e.fieldOfStudy ? ' - ' : ''}{e.institution} <span className="font-normal text-slate-500">{e.period}</span></div>)}
      {r.skills.length > 0 && <><div className={h}>Skills</div><p className="mt-1">{r.skills.join(', ')}</p></>}
      {r.projects.length > 0 && <div className={h}>Projects</div>}
      {r.projects.map((p, i) => <div key={i} className="mt-1.5"><span className="font-semibold">{p.name}</span> {p.url && <span className="text-slate-500">({p.url})</span>}<p>{p.description}</p></div>)}
      {r.certifications.length > 0 && <div className={h}>Certifications</div>}
      {r.certifications.map((c, i) => <div key={i} className="mt-1">{'\u2022'} {[c.name, c.issuer, c.year].filter(Boolean).join(', ')}</div>)}
      {r.achievements.length > 0 && <div className={h}>Achievements</div>}
      {r.achievements.map((a, i) => <div key={i} className="mt-1">{'\u2022'} {a}</div>)}
    </div>
  );
}

export default function ResumeBuilder() {
  const [r, setR] = useState<ResumeBuildInput>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState<Resume | null>(null);
  const [busy, setBusy] = useState(false);
  const [achievementText, setAchievementText] = useState('');

  const upd = (patch: Partial<ResumeBuildInput>) => setR((x) => ({ ...x, ...patch }));
  const personal = (k: keyof ResumeBuildInput['personal']) => (e: { target: { value: string } }) => upd({ personal: { ...r.personal, [k]: e.target.value } });

  const fillFromProfile = async () => {
    try {
      const p = await profileApi.get();
      upd({
        title: r.title || `${p.fullName} - Resume`,
        personal: { fullName: p.fullName, email: p.email, phone: p.phone ?? '', location: p.location ?? '', headline: p.headline ?? '', links: r.personal.links },
        summary: p.careerObjective ?? r.summary,
        skills: p.skills,
        education: p.education.map((e) => ({ institution: e.institution, degree: e.degree ?? '', fieldOfStudy: e.fieldOfStudy ?? '', period: [e.startYear, e.endYear].filter(Boolean).join(' - '), description: e.description ?? '' })),
        experience: p.experience.map((e) => ({ company: e.company, title: e.title, startDate: e.startDate ?? '', endDate: e.endDate ?? '', current: e.current, description: e.description ?? '' })),
      });
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  const save = async () => {
    setError(null);
    if (!r.title.trim()) return setError('Give your resume a title.');
    if (!r.personal.fullName.trim()) return setError('Your full name is required.');
    setBusy(true);
    try {
      const payload: ResumeBuildInput = { ...r, achievements: [...r.achievements, ...achievementText.split('\n').map((s) => s.trim()).filter(Boolean)] };
      const res = await resumeApi.build(payload);
      setSaved(res);
      await resumeApi.download(res.id, res.originalFilename);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <PageHeader title="Resume Builder" subtitle="Enter your real details, preview live and export a PDF. Nothing is invented for you."
        actions={<><button className="btn-secondary" onClick={fillFromProfile}>Fill from my profile</button><button className="btn-accent" onClick={save} disabled={busy}><Download className="h-4 w-4" />{busy ? 'Creating PDF...' : 'Save and export PDF'}</button></>} />
      {error && <div className="mb-4"><Alert>{error}</Alert></div>}
      {saved && <div className="mb-4"><Alert kind="success">Saved as "{saved.title}". <Link className="font-semibold underline" to="/resumes">Open Resume Studio</Link> to analyse it.</Alert></div>}

      <div className="grid gap-6 xl:grid-cols-2">
        <div className="space-y-5">
          <section className="card space-y-3 p-5">
            <h2 className="font-semibold">Basics</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <div><label className="label" htmlFor="rb-title">Resume title</label><input id="rb-title" className="input" value={r.title} onChange={(e) => upd({ title: e.target.value })} /></div>
              <div><label className="label" htmlFor="rb-tpl">Template</label><select id="rb-tpl" className="input" value={r.template} onChange={(e) => upd({ template: e.target.value as 'classic' | 'modern' })}><option value="modern">Modern</option><option value="classic">Classic</option></select></div>
              <div><label className="label" htmlFor="rb-name">Full name</label><input id="rb-name" className="input" value={r.personal.fullName} onChange={personal('fullName')} /></div>
              <div><label className="label" htmlFor="rb-head">Professional headline</label><input id="rb-head" className="input" value={r.personal.headline} onChange={personal('headline')} /></div>
              <div><label className="label" htmlFor="rb-email">Email</label><input id="rb-email" className="input" value={r.personal.email} onChange={personal('email')} /></div>
              <div><label className="label" htmlFor="rb-phone">Phone</label><input id="rb-phone" className="input" value={r.personal.phone} onChange={personal('phone')} /></div>
              <div><label className="label" htmlFor="rb-loc">Location</label><input id="rb-loc" className="input" value={r.personal.location} onChange={personal('location')} /></div>
              <div><label className="label" htmlFor="rb-links">Links (LinkedIn, portfolio)</label><input id="rb-links" className="input" value={r.personal.links} onChange={personal('links')} /></div>
            </div>
            <div><label className="label" htmlFor="rb-sum">Professional summary</label><textarea id="rb-sum" className="input" rows={4} value={r.summary} onChange={(e) => upd({ summary: e.target.value })} />
              <SuggestBox section="summary" content={r.summary ?? ''} role={r.personal.headline} onApply={(t) => upd({ summary: t })} /></div>
          </section>

          <section className="card space-y-3 p-5">
            <div className="flex items-center justify-between"><h2 className="font-semibold">Work experience</h2><button className="btn-secondary btn-sm" onClick={() => upd({ experience: [...r.experience, { company: '', title: '', startDate: '', endDate: '', current: false, description: '' }] })}><Plus className="h-4 w-4" />Add</button></div>
            {r.experience.map((e, i) => {
              const set = (patch: Partial<typeof e>) => upd({ experience: r.experience.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
              return (
                <div key={i} className="space-y-2 rounded-xl border border-slate-200 p-3">
                  <div className="grid gap-2 sm:grid-cols-2">
                    <input aria-label={`Job title ${i + 1}`} className="input" placeholder="Job title" value={e.title} onChange={(ev) => set({ title: ev.target.value })} />
                    <input aria-label={`Company ${i + 1}`} className="input" placeholder="Company" value={e.company} onChange={(ev) => set({ company: ev.target.value })} />
                    <input aria-label={`Start date ${i + 1}`} className="input" placeholder="Start (e.g. Jan 2022)" value={e.startDate} onChange={(ev) => set({ startDate: ev.target.value })} />
                    <input aria-label={`End date ${i + 1}`} className="input" placeholder="End" disabled={e.current} value={e.endDate} onChange={(ev) => set({ endDate: ev.target.value })} />
                  </div>
                  <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={e.current} onChange={(ev) => set({ current: ev.target.checked })} />I currently work here</label>
                  <textarea aria-label={`Description ${i + 1}`} className="input" rows={3} placeholder="One achievement or responsibility per line" value={e.description} onChange={(ev) => set({ description: ev.target.value })} />
                  <SuggestBox section="experience" content={e.description ?? ''} role={e.title} onApply={(t) => set({ description: t })} />
                  <button className="btn-ghost btn-sm text-red-600" onClick={() => upd({ experience: r.experience.filter((_, j) => j !== i) })}><Trash2 className="h-4 w-4" />Remove</button>
                </div>
              );
            })}
          </section>

          <section className="card space-y-3 p-5">
            <div className="flex items-center justify-between"><h2 className="font-semibold">Education</h2><button className="btn-secondary btn-sm" onClick={() => upd({ education: [...r.education, { institution: '', degree: '', fieldOfStudy: '', period: '', description: '' }] })}><Plus className="h-4 w-4" />Add</button></div>
            {r.education.map((e, i) => {
              const set = (patch: Partial<typeof e>) => upd({ education: r.education.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
              return (
                <div key={i} className="grid gap-2 rounded-xl border border-slate-200 p-3 sm:grid-cols-2">
                  <input aria-label={`Institution ${i + 1}`} className="input" placeholder="Institution" value={e.institution} onChange={(ev) => set({ institution: ev.target.value })} />
                  <input aria-label={`Degree ${i + 1}`} className="input" placeholder="Degree" value={e.degree} onChange={(ev) => set({ degree: ev.target.value })} />
                  <input aria-label={`Field ${i + 1}`} className="input" placeholder="Field of study" value={e.fieldOfStudy} onChange={(ev) => set({ fieldOfStudy: ev.target.value })} />
                  <input aria-label={`Period ${i + 1}`} className="input" placeholder="Period (e.g. 2019 - 2023)" value={e.period} onChange={(ev) => set({ period: ev.target.value })} />
                  <button className="btn-ghost btn-sm justify-self-start text-red-600" onClick={() => upd({ education: r.education.filter((_, j) => j !== i) })}><Trash2 className="h-4 w-4" />Remove</button>
                </div>
              );
            })}
          </section>

          <section className="card space-y-3 p-5">
            <h2 className="font-semibold">Skills</h2>
            <TagInput value={r.skills} onChange={(skills) => upd({ skills })} placeholder="Type a skill and press Enter" />
          </section>

          <section className="card space-y-3 p-5">
            <div className="flex items-center justify-between"><h2 className="font-semibold">Projects</h2><button className="btn-secondary btn-sm" onClick={() => upd({ projects: [...r.projects, { name: '', description: '', url: '' }] })}><Plus className="h-4 w-4" />Add</button></div>
            {r.projects.map((p, i) => {
              const set = (patch: Partial<typeof p>) => upd({ projects: r.projects.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
              return (
                <div key={i} className="space-y-2 rounded-xl border border-slate-200 p-3">
                  <input aria-label={`Project name ${i + 1}`} className="input" placeholder="Project name" value={p.name} onChange={(ev) => set({ name: ev.target.value })} />
                  <input aria-label={`Project link ${i + 1}`} className="input" placeholder="Link (optional)" value={p.url} onChange={(ev) => set({ url: ev.target.value })} />
                  <textarea aria-label={`Project description ${i + 1}`} className="input" rows={2} placeholder="What you built and the outcome" value={p.description} onChange={(ev) => set({ description: ev.target.value })} />
                  <button className="btn-ghost btn-sm text-red-600" onClick={() => upd({ projects: r.projects.filter((_, j) => j !== i) })}><Trash2 className="h-4 w-4" />Remove</button>
                </div>
              );
            })}
          </section>

          <section className="card space-y-3 p-5">
            <div className="flex items-center justify-between"><h2 className="font-semibold">Certifications</h2><button className="btn-secondary btn-sm" onClick={() => upd({ certifications: [...r.certifications, { name: '', issuer: '', year: '' }] })}><Plus className="h-4 w-4" />Add</button></div>
            {r.certifications.map((c, i) => {
              const set = (patch: Partial<typeof c>) => upd({ certifications: r.certifications.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
              return (
                <div key={i} className="grid gap-2 sm:grid-cols-[2fr_2fr_1fr_auto]">
                  <input aria-label={`Certification ${i + 1}`} className="input" placeholder="Name" value={c.name} onChange={(ev) => set({ name: ev.target.value })} />
                  <input aria-label={`Issuer ${i + 1}`} className="input" placeholder="Issuer" value={c.issuer} onChange={(ev) => set({ issuer: ev.target.value })} />
                  <input aria-label={`Year ${i + 1}`} className="input" placeholder="Year" value={c.year} onChange={(ev) => set({ year: ev.target.value })} />
                  <button className="btn-ghost btn-sm text-red-600" aria-label="Remove certification" onClick={() => upd({ certifications: r.certifications.filter((_, j) => j !== i) })}><Trash2 className="h-4 w-4" /></button>
                </div>
              );
            })}
          </section>

          <section className="card space-y-2 p-5">
            <h2 className="font-semibold">Achievements</h2>
            <textarea aria-label="Achievements" className="input" rows={3} placeholder="One achievement per line" value={achievementText} onChange={(e) => setAchievementText(e.target.value)} />
          </section>
        </div>

        <div className="xl:sticky xl:top-6 xl:self-start">
          <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-slate-600"><Sparkles className="h-4 w-4 text-accent-600" />Live preview</div>
          <Preview r={{ ...r, achievements: [...r.achievements, ...achievementText.split('\n').map((s) => s.trim()).filter(Boolean)] }} />
        </div>
      </div>
    </div>
  );
}
