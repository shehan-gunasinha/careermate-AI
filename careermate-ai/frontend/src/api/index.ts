import api from './client';
import type {
  ApiResponse, Application, ApplicationHistory, ApplicationInput, ApplicationStatus, AppNotification, AssistantReply,
  AuthResponse, ChatMessage, Conversation, Dashboard, GeminiKeyStatus, Job, JobFilters, MatchResult, PageResponse,
  Preferences, Profile, Resume, ResumeAnalysis, ResumeBuildInput, User,
} from './types';

const data = <T>(p: Promise<{ data: ApiResponse<T> }>): Promise<T> => p.then((r) => r.data.data);

const clean = (f: object) => Object.fromEntries(Object.entries(f).filter(([, v]) => v !== undefined && v !== ''));

export const authApi = {
  login: (email: string, password: string) => data<AuthResponse>(api.post('/auth/login', { email, password })),
  register: (fullName: string, email: string, password: string, confirmPassword: string) =>
    data<AuthResponse>(api.post('/auth/register', { fullName, email, password, confirmPassword })),
  me: () => data<User>(api.get('/auth/me')),
  logout: () => data<null>(api.post('/auth/logout')),
  changePassword: (currentPassword: string, newPassword: string, confirmPassword: string) =>
    data<null>(api.put('/auth/password', { currentPassword, newPassword, confirmPassword })),
  forgotPassword: (email: string) => data<null>(api.post('/auth/forgot-password', { email })),
  resetPassword: (email: string, otp: string, newPassword: string, confirmPassword: string) =>
    data<null>(api.post('/auth/reset-password', { email, otp, newPassword, confirmPassword })),
};

export const dashboardApi = { get: () => data<Dashboard>(api.get('/dashboard')) };

export const profileApi = {
  get: () => data<Profile>(api.get('/profile')),
  update: (p: Record<string, unknown>) => data<Profile>(api.put('/profile', p)),
  updatePreferences: (p: Preferences) => data<Preferences>(api.put('/profile/preferences', p)),
  uploadAvatar: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return data<Profile>(api.post('/profile/avatar', form));
  },
  removeAvatar: () => data<Profile>(api.delete('/profile/avatar')),
};

export const jobsApi = {
  search: (f: JobFilters) => data<PageResponse<Job>>(api.get('/jobs', { params: clean(f) })),
  get: (id: number) => data<Job>(api.get(`/jobs/${id}`)),
  recommended: (page = 0, size = 12) => data<PageResponse<Job>>(api.get('/jobs/recommended', { params: { page, size } })),
  match: (id: number) => data<MatchResult>(api.get(`/jobs/${id}/match`)),
  save: (id: number) => data<null>(api.post(`/jobs/${id}/save`)),
  unsave: (id: number) => data<null>(api.delete(`/jobs/${id}/save`)),
  saved: (page = 0, size = 12) => data<PageResponse<Job>>(api.get('/jobs/saved', { params: { page, size } })),
};

export const resumeApi = {
  list: () => data<Resume[]>(api.get('/resumes')),
  get: (id: number) => data<Resume>(api.get(`/resumes/${id}`)),
  upload: (file: File, title?: string) => {
    const form = new FormData();
    form.append('file', file);
    if (title) form.append('title', title);
    return data<Resume>(api.post('/resumes/upload', form));
  },
  remove: (id: number) => data<null>(api.delete(`/resumes/${id}`)),
  analyze: (id: number) => data<ResumeAnalysis>(api.post(`/resumes/${id}/analyze`)),
  analysis: (id: number) => data<ResumeAnalysis>(api.get(`/resumes/${id}/analysis`)),
  history: (id: number) => data<ResumeAnalysis[]>(api.get(`/resumes/${id}/analyses`)),
  build: (input: ResumeBuildInput) => data<Resume>(api.post('/resumes/build', input)),
  suggest: (section: string, content: string, targetRole?: string) =>
    data<{ suggestion: string; aiPowered: boolean }>(api.post('/resumes/suggest', { section, content, targetRole })),
  download: async (id: number, filename: string) => {
    const res = await api.get(`/resumes/${id}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(res.data as Blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};

export const applicationApi = {
  list: (params: { status?: string; q?: string; page?: number; size?: number } = {}) =>
    data<PageResponse<Application>>(api.get('/applications', { params: { size: 200, ...clean(params) } })),
  create: (input: ApplicationInput) => data<Application>(api.post('/applications', input)),
  update: (id: number, input: ApplicationInput) => data<Application>(api.put(`/applications/${id}`, input)),
  setStatus: (id: number, status: ApplicationStatus, note?: string) =>
    data<Application>(api.patch(`/applications/${id}/status`, { status, note })),
  remove: (id: number) => data<null>(api.delete(`/applications/${id}`)),
  history: (id: number) => data<ApplicationHistory[]>(api.get(`/applications/${id}/history`)),
};

export const notificationApi = {
  list: (unreadOnly = false, page = 0, size = 30) =>
    data<PageResponse<AppNotification>>(api.get('/notifications', { params: { unreadOnly, page, size } })),
  unreadCount: () => data<{ count: number }>(api.get('/notifications/unread-count')),
  read: (id: number) => data<AppNotification>(api.patch(`/notifications/${id}/read`)),
  readAll: () => data<{ updated: number }>(api.patch('/notifications/read-all')),
};

export const geminiKeyApi = {
  status: () => data<GeminiKeyStatus>(api.get('/ai/gemini-key')),
  save: (apiKey: string) => data<GeminiKeyStatus>(api.put('/ai/gemini-key', { apiKey })),
  remove: () => data<GeminiKeyStatus>(api.delete('/ai/gemini-key')),
};

export const assistantApi = {
  status: () => data<{ aiEnabled: boolean; mode: string }>(api.get('/career-assistant/status')),
  send: (message: string, conversationId?: number | null, useProfile = true) =>
    data<AssistantReply>(api.post('/career-assistant', { message, conversationId: conversationId ?? undefined, useProfile })),
  conversations: () => data<Conversation[]>(api.get('/career-assistant/conversations')),
  messages: (id: number) => data<ChatMessage[]>(api.get(`/career-assistant/conversations/${id}/messages`)),
  remove: (id: number) => data<null>(api.delete(`/career-assistant/conversations/${id}`)),
};
