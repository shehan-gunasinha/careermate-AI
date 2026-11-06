import type { ApplicationStatus, EmploymentType, ExperienceLevel, WorkArrangement } from '../api/types';

export const EMPLOYMENT_TYPES: { value: EmploymentType; label: string }[] = [
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'FREELANCE', label: 'Freelance' },
  { value: 'TEMPORARY', label: 'Temporary' },
];

export const EXPERIENCE_LEVELS: { value: ExperienceLevel; label: string }[] = [
  { value: 'ENTRY', label: 'Entry-level' },
  { value: 'JUNIOR', label: 'Junior' },
  { value: 'MID', label: 'Mid-level' },
  { value: 'SENIOR', label: 'Senior' },
  { value: 'EXECUTIVE', label: 'Executive' },
];

export const WORK_ARRANGEMENTS: { value: WorkArrangement; label: string }[] = [
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
  { value: 'ON_SITE', label: 'On-site' },
];

export const INDUSTRIES = [
  'Information Technology', 'Engineering', 'Finance and Accounting', 'Marketing', 'Business Management', 'Healthcare',
  'Education', 'Administration', 'Hospitality', 'Customer Service', 'Human Resources', 'Design',
];

export const APPLICATION_STATUSES: { value: ApplicationStatus; label: string; color: string }[] = [
  { value: 'SAVED', label: 'Saved', color: 'bg-slate-100 text-slate-700' },
  { value: 'APPLIED', label: 'Applied', color: 'bg-blue-100 text-blue-700' },
  { value: 'UNDER_REVIEW', label: 'Under Review', color: 'bg-indigo-100 text-indigo-700' },
  { value: 'SHORTLISTED', label: 'Shortlisted', color: 'bg-purple-100 text-purple-700' },
  { value: 'INTERVIEW_SCHEDULED', label: 'Interview Scheduled', color: 'bg-amber-100 text-amber-800' },
  { value: 'ASSESSMENT', label: 'Assessment', color: 'bg-orange-100 text-orange-700' },
  { value: 'OFFER_RECEIVED', label: 'Offer Received', color: 'bg-emerald-100 text-emerald-700' },
  { value: 'REJECTED', label: 'Rejected', color: 'bg-red-100 text-red-700' },
  { value: 'WITHDRAWN', label: 'Withdrawn', color: 'bg-zinc-200 text-zinc-700' },
];

export const statusMeta = (s: string) =>
  APPLICATION_STATUSES.find((x) => x.value === s) ?? { value: s, label: s, color: 'bg-slate-100 text-slate-700' };

export const label = (list: { value: string; label: string }[], value?: string | null) =>
  list.find((x) => x.value === value)?.label ?? value ?? '';

export function formatSalary(min?: number | null, max?: number | null, currency?: string | null): string | null {
  if (min == null && max == null) return null;
  const fmt = (n: number) => n.toLocaleString('en-US', { maximumFractionDigits: 0 });
  const cur = currency ? `${currency} ` : '';
  if (min != null && max != null) return `${cur}${fmt(min)} - ${fmt(max)}`;
  return `${cur}${fmt((min ?? max) as number)}${min != null ? '+' : ''}`;
}

export function formatDate(value?: string | null): string {
  if (!value) return '';
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '';
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? value
    : d.toLocaleString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function timeAgo(value?: string | null): string {
  if (!value) return '';
  const diff = Date.now() - new Date(value).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  return d < 30 ? `${d}d ago` : formatDate(value);
}

export const scoreColor = (n: number) => (n >= 75 ? 'text-emerald-600' : n >= 50 ? 'text-amber-600' : 'text-red-600');
export const scoreBg = (n: number) => (n >= 75 ? 'bg-emerald-500' : n >= 50 ? 'bg-amber-500' : 'bg-red-500');
