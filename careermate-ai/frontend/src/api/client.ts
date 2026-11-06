import axios, { AxiosError } from 'axios';

export const TOKEN_KEY = 'careermate_token';

/**
 * API base URL. Relative by default so it works through the Vite dev proxy locally and through the Nginx
 * reverse proxy in Docker. Override with VITE_API_BASE_URL at build time if the API lives elsewhere.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 60000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const url = error.config?.url ?? '';
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/register');
    if (error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem(TOKEN_KEY);
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

interface ErrorBody {
  message?: string;
  data?: Record<string, string> | null;
}

export function errorMessage(e: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (axios.isAxiosError(e)) {
    const body = e.response?.data as ErrorBody | undefined;
    if (body?.data && typeof body.data === 'object') {
      const first = Object.values(body.data)[0];
      if (typeof first === 'string') return first;
    }
    if (body?.message) return body.message;
    if (e.code === 'ECONNABORTED') return 'The request timed out. Please try again.';
    if (!e.response) return 'Cannot reach the server. Check your connection.';
  }
  return fallback;
}

export function fieldErrors(e: unknown): Record<string, string> {
  if (axios.isAxiosError(e)) {
    const body = e.response?.data as ErrorBody | undefined;
    if (body?.data && typeof body.data === 'object') return body.data as Record<string, string>;
  }
  return {};
}

export default api;
