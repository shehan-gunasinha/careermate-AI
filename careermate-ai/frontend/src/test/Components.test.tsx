import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import type { Job } from '../api/types';
import JobCard from '../components/JobCard';
import ProtectedRoute from '../components/ProtectedRoute';
import { SimpleMarkdown } from '../components/SimpleMarkdown';
import { ScoreRing } from '../components/ui';
import { AuthProvider } from '../context/AuthContext';
import { validateRegistration } from '../pages/Register';
import { formatSalary } from '../lib/format';

const job: Job = {
  id: 7, title: 'Junior Software Engineer', companyName: 'Sample Tech (Demo)', location: 'Colombo', industry: 'IT',
  employmentType: 'FULL_TIME', workArrangement: 'HYBRID', experienceLevel: 'JUNIOR', salaryMin: 18000, salaryMax: 30000, salaryCurrency: 'USD',
  shortDescription: 'Build APIs', description: 'desc', requiredSkills: ['Java', 'SQL'], preferredSkills: [], postedDate: '2026-10-01',
  deadline: null, applicationUrl: 'https://example.com/apply', source: 'DEMO', demo: true, active: true, saved: false, matchScore: 82,
};

describe('JobCard', () => {
  it('shows the key job details, demo badge and match score', () => {
    render(<MemoryRouter><JobCard job={job} /></MemoryRouter>);
    expect(screen.getByText('Junior Software Engineer')).toBeInTheDocument();
    expect(screen.getByText('Sample Tech (Demo)')).toBeInTheDocument();
    expect(screen.getByText(/USD 18,000 - 30,000/)).toBeInTheDocument();
    expect(screen.getByText('Demo listing')).toBeInTheDocument();
    expect(screen.getByLabelText(/match 82 percent/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /apply/i })).toHaveAttribute('href', 'https://example.com/apply');
  });

  it('calls onToggleSave when the save button is pressed', async () => {
    const onToggle = vi.fn();
    render(<MemoryRouter><JobCard job={job} onToggleSave={onToggle} /></MemoryRouter>);
    await userEvent.click(screen.getByRole('button', { name: /save/i }));
    expect(onToggle).toHaveBeenCalledWith(job);
  });
});

describe('ProtectedRoute', () => {
  it('redirects unauthenticated visitors to the login page', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<div>Login screen</div>} />
            <Route element={<ProtectedRoute />}><Route path="/dashboard" element={<div>Secret</div>} /></Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    );
    expect(screen.getByText('Login screen')).toBeInTheDocument();
    expect(screen.queryByText('Secret')).not.toBeInTheDocument();
  });
});

describe('helpers', () => {
  it('renders markdown lists and bold text safely', () => {
    render(<SimpleMarkdown text={'**Title**\n- one\n- two <script>alert(1)</script>\n1. first'} />);
    expect(screen.getByText('Title').tagName).toBe('STRONG');
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
    expect(document.querySelector('script')).toBeNull();
  });

  it('exposes the score on the ring', () => {
    render(<ScoreRing value={64} label="Resume score" />);
    expect(screen.getByLabelText('Resume score 64 percent')).toBeInTheDocument();
  });

  it('validates the registration form', () => {
    expect(validateRegistration({ fullName: '', email: 'bad', password: 'short', confirmPassword: 'x' })).toMatchObject({
      fullName: expect.any(String), email: expect.any(String), password: expect.any(String), confirmPassword: expect.any(String),
    });
    expect(validateRegistration({ fullName: 'A B', email: 'a@b.co', password: 'Passw0rd1', confirmPassword: 'Passw0rd1' })).toEqual({});
  });

  it('formats salaries', () => {
    expect(formatSalary(null, null, 'USD')).toBeNull();
    expect(formatSalary(5000, null, 'LKR')).toBe('LKR 5,000+');
  });
});
