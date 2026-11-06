import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Camera, Plus, Trash2 } from 'lucide-react';
import { profileApi } from '../api';
import { errorMessage, fieldErrors } from '../api/client';
import type { EmploymentStatus, Profile as ProfileData } from '../api/types';
import { Alert, Modal, PageHeader, ProgressBar, Spinner, TagInput } from '../components/ui';
import { useAuth } from '../context/AuthContext';
import { INDUSTRIES } from '../lib/format';

const STATUSES: { value: EmploymentStatus; label: string }[] = [
  { value: 'EMPLOYED', label: 'Employed' }, { value: 'UNEMPLOYED', label: 'Unemployed / looking' }, { value: 'STUDENT', label: 'Student' },
  { value: 'SELF_EMPLOYED', label: 'Self-employed' }, { value: 'OTHER', label: 'Other' },
];

export default function Profile() {
  const { user, setUser } = useAuth();
  const [p, setP] = useState<ProfileData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [avatarBusy, setAvatarBusy] = useState(false);
  const [avatarConfirm, setAvatarConfirm] = useState<{ kind: 'upload'; file: File } | { kind: 'remove' } | null>(null);
  const avatarInput = useRef<HTMLInputElement>(null);

  useEffect(() => { profileApi.get().then(setP).catch((e) => setError(errorMessage(e))); }, []);

  if (!p) return error ? <Alert>{error}</Alert> : <Spinner />;

  const initials = p.fullName.trim().split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('') || '?';
  const upd = (patch: Partial<ProfileData>) => setP({ ...p, ...patch });
  const txt = (k: 'fullName' | 'phone' | 'location' | 'headline' | 'careerObjective') => (e: { target: { value: string } }) => upd({ [k]: e.target.value });

  const save = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setNotice(null);
    setErrors({});
    if (!p.fullName.trim()) return setErrors({ fullName: 'Full name is required' });
    setSaving(true);
    try {
      const saved = await profileApi.update({
        fullName: p.fullName, phone: p.phone, location: p.location, headline: p.headline, careerObjective: p.careerObjective,
        employmentStatus: p.employmentStatus || null, yearsOfExperience: p.yearsOfExperience === undefined || p.yearsOfExperience === null || Number.isNaN(p.yearsOfExperience) ? null : p.yearsOfExperience,
        skills: p.skills,
        education: p.education.filter((x) => x.institution.trim()),
        experience: p.experience.filter((x) => x.company.trim() && x.title.trim()).map((x) => ({ ...x, startDate: x.startDate || null, endDate: x.current ? null : x.endDate || null })),
      });
      const prefs = await profileApi.updatePreferences(p.preferences);
      setP({ ...saved, preferences: prefs });
      if (user) setUser({ ...user, fullName: saved.fullName });
      setNotice('Profile saved.');
      window.scrollTo?.({ top: 0, behavior: 'smooth' });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const chooseAvatar = (file?: File) => {
    if (!file) return;
    setError(null);
    setNotice(null);
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      setError('Choose a PNG, JPG or WEBP image.');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setError('Image is too large. The limit is 2 MB.');
      return;
    }
    setAvatarConfirm({ kind: 'upload', file });
  };

  const uploadAvatar = async (file: File) => {
    setAvatarBusy(true);
    try {
      const saved = await profileApi.uploadAvatar(file);
      setP(saved);
      if (user) setUser({ ...user, avatarUrl: saved.avatarUrl });
      setNotice('Profile picture updated.');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setAvatarBusy(false);
    }
  };

  const removeAvatar = async () => {
    setAvatarBusy(true);
    try {
      const saved = await profileApi.removeAvatar();
      setP(saved);
      if (user) setUser({ ...user, avatarUrl: saved.avatarUrl });
      setNotice('Profile picture removed.');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setAvatarBusy(false);
    }
  };

  const confirmAvatar = async () => {
    if (!avatarConfirm) return;
    const action = avatarConfirm;
    setAvatarConfirm(null);
    setError(null);
    setNotice(null);
    if (action.kind === 'upload') await uploadAvatar(action.file);
    else await removeAvatar();
  };

  return (
    <form onSubmit={save} noValidate>
      <PageHeader title="My Profile" subtitle="Complete profiles get better job matches and AI advice." actions={<button className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save profile'}</button>} />
      {error && <div className="mb-4"><Alert>{error}</Alert></div>}
      {notice && <div className="mb-4"><Alert kind="success">{notice}</Alert></div>}

      <section className="card mb-6 flex flex-col items-center gap-5 p-5 sm:flex-row">
        <div className="shrink-0">
          {p.avatarUrl ? (
            <img src={p.avatarUrl} alt={`${p.fullName} profile picture`} className="h-24 w-24 rounded-full border-4 border-white object-cover shadow ring-1 ring-slate-200" />
          ) : (
            <div className="flex h-24 w-24 items-center justify-center rounded-full bg-navy-100 text-2xl font-bold text-navy-700 ring-1 ring-slate-200" aria-hidden>{initials}</div>
          )}
        </div>
        <div className="flex-1 text-center sm:text-left">
          <h2 className="font-semibold">Profile picture</h2>
          <p className="mt-1 text-sm text-slate-500">Upload a PNG, JPG or WEBP image up to 2 MB. It appears next to your name.</p>
          <div className="mt-3 flex flex-wrap justify-center gap-2 sm:justify-start">
            <button type="button" className="btn-secondary btn-sm" onClick={() => avatarInput.current?.click()} disabled={avatarBusy}>
              <Camera className="h-4 w-4" />{avatarBusy ? 'Uploading...' : p.avatarUrl ? 'Change photo' : 'Upload photo'}
            </button>
            {p.avatarUrl && (
              <button type="button" className="btn-ghost btn-sm text-red-600" onClick={() => setAvatarConfirm({ kind: 'remove' })} disabled={avatarBusy}>
                <Trash2 className="h-4 w-4" />Remove
              </button>
            )}
          </div>
          <input
            ref={avatarInput} type="file" accept="image/png,image/jpeg,image/webp" className="hidden"
            onChange={(e) => { chooseAvatar(e.target.files?.[0]); e.target.value = ''; }}
          />
        </div>
      </section>

      {avatarConfirm && (
        <Modal
          title={avatarConfirm.kind === 'upload' ? 'Update profile picture?' : 'Remove profile picture?'}
          onClose={() => setAvatarConfirm(null)}
        >
          <p className="text-sm text-slate-600">
            {avatarConfirm.kind === 'upload'
              ? `Use "${avatarConfirm.file.name}" as your profile picture? Your current picture will be replaced.`
              : 'Remove your profile picture? Your initials will be shown until you upload a new one.'}
          </p>
          <div className="mt-5 flex justify-end gap-2">
            <button type="button" className="btn-secondary" onClick={() => setAvatarConfirm(null)} disabled={avatarBusy}>Cancel</button>
            <button
              type="button"
              className={avatarConfirm.kind === 'remove' ? 'btn-secondary text-red-600' : 'btn-primary'}
              onClick={confirmAvatar}
              disabled={avatarBusy}
            >
              {avatarBusy ? 'Please wait...' : avatarConfirm.kind === 'remove' ? 'Remove' : 'Update photo'}
            </button>
          </div>
        </Modal>
      )}

      <div className="card mb-6 p-5"><div className="mb-2 flex justify-between text-sm"><span className="font-medium">Profile completion</span><span>{p.completionPercentage}%</span></div><ProgressBar value={p.completionPercentage} /></div>

      <div className="space-y-6">
        <section className="card grid gap-4 p-5 sm:grid-cols-2">
          <h2 className="font-semibold sm:col-span-2">Personal information</h2>
          <div><label className="label" htmlFor="p-name">Full name</label><input id="p-name" className="input" value={p.fullName} onChange={txt('fullName')} />{errors.fullName && <p className="field-error">{errors.fullName}</p>}</div>
          <div><label className="label" htmlFor="p-email">Email</label><input id="p-email" className="input bg-slate-50" value={p.email} readOnly /></div>
          <div><label className="label" htmlFor="p-phone">Phone number</label><input id="p-phone" className="input" value={p.phone ?? ''} onChange={txt('phone')} /></div>
          <div><label className="label" htmlFor="p-loc">Location</label><input id="p-loc" className="input" value={p.location ?? ''} onChange={txt('location')} /></div>
          <div className="sm:col-span-2"><label className="label" htmlFor="p-head">Professional headline</label><input id="p-head" className="input" value={p.headline ?? ''} onChange={txt('headline')} placeholder="e.g. Junior Accountant | Excel | IFRS" /></div>
          <div className="sm:col-span-2"><label className="label" htmlFor="p-obj">Career objective</label><textarea id="p-obj" rows={3} className="input" value={p.careerObjective ?? ''} onChange={txt('careerObjective')} /></div>
          <div><label className="label" htmlFor="p-status">Current employment status</label>
            <select id="p-status" className="input" value={p.employmentStatus ?? ''} onChange={(e) => upd({ employmentStatus: (e.target.value || null) as EmploymentStatus | null })}><option value="">Select...</option>{STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}</select></div>
          <div><label className="label" htmlFor="p-years">Years of experience</label><input id="p-years" type="number" min={0} max={60} className="input" value={p.yearsOfExperience ?? ''} onChange={(e) => upd({ yearsOfExperience: e.target.value === '' ? null : Number(e.target.value) })} />{errors.yearsOfExperience && <p className="field-error">{errors.yearsOfExperience}</p>}</div>
        </section>

        <section className="card space-y-3 p-5"><h2 className="font-semibold">Skills</h2><TagInput id="p-skills" value={p.skills} onChange={(skills) => upd({ skills })} placeholder="Add a skill and press Enter" /></section>

        <section className="card space-y-3 p-5">
          <div className="flex items-center justify-between"><h2 className="font-semibold">Education</h2><button type="button" className="btn-secondary btn-sm" onClick={() => upd({ education: [...p.education, { institution: '', degree: '', fieldOfStudy: '', startYear: null, endYear: null, description: '' }] })}><Plus className="h-4 w-4" />Add</button></div>
          {p.education.map((e, i) => {
            const set = (patch: Partial<typeof e>) => upd({ education: p.education.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
            return (
              <div key={i} className="grid gap-2 rounded-xl border border-slate-200 p-3 sm:grid-cols-2">
                <input aria-label={`Institution ${i + 1}`} className="input" placeholder="Institution" value={e.institution} onChange={(ev) => set({ institution: ev.target.value })} />
                <input aria-label={`Degree ${i + 1}`} className="input" placeholder="Degree / qualification" value={e.degree ?? ''} onChange={(ev) => set({ degree: ev.target.value })} />
                <input aria-label={`Field of study ${i + 1}`} className="input" placeholder="Field of study" value={e.fieldOfStudy ?? ''} onChange={(ev) => set({ fieldOfStudy: ev.target.value })} />
                <div className="flex gap-2">
                  <input aria-label={`Start year ${i + 1}`} type="number" className="input" placeholder="Start year" value={e.startYear ?? ''} onChange={(ev) => set({ startYear: ev.target.value ? Number(ev.target.value) : null })} />
                  <input aria-label={`End year ${i + 1}`} type="number" className="input" placeholder="End year" value={e.endYear ?? ''} onChange={(ev) => set({ endYear: ev.target.value ? Number(ev.target.value) : null })} />
                </div>
                <button type="button" className="btn-ghost btn-sm justify-self-start text-red-600" onClick={() => upd({ education: p.education.filter((_, j) => j !== i) })}><Trash2 className="h-4 w-4" />Remove</button>
              </div>
            );
          })}
        </section>

        <section className="card space-y-3 p-5">
          <div className="flex items-center justify-between"><h2 className="font-semibold">Work experience</h2><button type="button" className="btn-secondary btn-sm" onClick={() => upd({ experience: [...p.experience, { company: '', title: '', startDate: '', endDate: '', current: false, description: '' }] })}><Plus className="h-4 w-4" />Add</button></div>
          {p.experience.map((e, i) => {
            const set = (patch: Partial<typeof e>) => upd({ experience: p.experience.map((x, j) => (j === i ? { ...x, ...patch } : x)) });
            return (
              <div key={i} className="grid gap-2 rounded-xl border border-slate-200 p-3 sm:grid-cols-2">
                <input aria-label={`Job title ${i + 1}`} className="input" placeholder="Job title" value={e.title} onChange={(ev) => set({ title: ev.target.value })} />
                <input aria-label={`Company ${i + 1}`} className="input" placeholder="Company" value={e.company} onChange={(ev) => set({ company: ev.target.value })} />
                <input aria-label={`Start date ${i + 1}`} type="date" className="input" value={e.startDate ?? ''} onChange={(ev) => set({ startDate: ev.target.value })} />
                <input aria-label={`End date ${i + 1}`} type="date" className="input" disabled={e.current} value={e.endDate ?? ''} onChange={(ev) => set({ endDate: ev.target.value })} />
                <label className="flex items-center gap-2 text-sm sm:col-span-2"><input type="checkbox" checked={e.current} onChange={(ev) => set({ current: ev.target.checked })} />I currently work here</label>
                <textarea aria-label={`Experience description ${i + 1}`} rows={2} className="input sm:col-span-2" placeholder="What you did and achieved" value={e.description ?? ''} onChange={(ev) => set({ description: ev.target.value })} />
                <button type="button" className="btn-ghost btn-sm justify-self-start text-red-600" onClick={() => upd({ experience: p.experience.filter((_, j) => j !== i) })}><Trash2 className="h-4 w-4" />Remove</button>
              </div>
            );
          })}
        </section>

        <section className="card space-y-4 p-5">
          <h2 className="font-semibold">Job preferences</h2>
          <div><label className="label" htmlFor="pref-ind">Preferred industries</label><TagInput id="pref-ind" value={p.preferences.preferredIndustries} onChange={(v) => upd({ preferences: { ...p.preferences, preferredIndustries: v } })} placeholder={`e.g. ${INDUSTRIES[0]}`} /></div>
          <div><label className="label" htmlFor="pref-roles">Preferred job roles</label><TagInput id="pref-roles" value={p.preferences.preferredRoles} onChange={(v) => upd({ preferences: { ...p.preferences, preferredRoles: v } })} placeholder="e.g. Business Analyst" /></div>
          <div><label className="label" htmlFor="pref-loc">Preferred locations</label><TagInput id="pref-loc" value={p.preferences.preferredLocations} onChange={(v) => upd({ preferences: { ...p.preferences, preferredLocations: v } })} placeholder="e.g. Colombo" /></div>
        </section>
        <div className="flex justify-end"><button className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save profile'}</button></div>
      </div>
    </form>
  );
}
