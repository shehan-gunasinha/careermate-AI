export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  avatarUrl?: string | null;
  role: 'JOB_SEEKER' | 'RECRUITER' | 'ADMIN';
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  user: User;
}

export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'INTERNSHIP' | 'CONTRACT' | 'FREELANCE' | 'TEMPORARY';
export type ExperienceLevel = 'ENTRY' | 'JUNIOR' | 'MID' | 'SENIOR' | 'EXECUTIVE';
export type WorkArrangement = 'REMOTE' | 'HYBRID' | 'ON_SITE';

export interface Job {
  id: number;
  title: string;
  companyName: string;
  companyLogoUrl?: string | null;
  companyDescription?: string | null;
  location: string;
  industry?: string | null;
  employmentType: EmploymentType;
  workArrangement: WorkArrangement;
  experienceLevel: ExperienceLevel;
  salaryMin?: number | null;
  salaryMax?: number | null;
  salaryCurrency?: string | null;
  shortDescription?: string | null;
  description: string;
  responsibilities?: string | null;
  requiredQualifications?: string | null;
  preferredQualifications?: string | null;
  requiredSkills: string[];
  preferredSkills: string[];
  postedDate?: string | null;
  deadline?: string | null;
  applicationUrl?: string | null;
  source: string;
  demo: boolean;
  active: boolean;
  saved: boolean;
  matchScore?: number | null;
}

export interface MatchResult {
  jobId: number;
  score: number;
  matchingSkills: string[];
  missingSkills: string[];
  skillScore: number;
  experienceScore: number;
  experienceNote: string;
  educationScore: number;
  educationNote: string;
  preferenceScore: number;
  strengths: string[];
  improvements: string[];
  recommendations: string[];
  aiPowered: boolean;
}

export type EmploymentStatus = 'EMPLOYED' | 'UNEMPLOYED' | 'STUDENT' | 'SELF_EMPLOYED' | 'OTHER';

export interface Education {
  id?: number | null;
  institution: string;
  degree?: string | null;
  fieldOfStudy?: string | null;
  startYear?: number | null;
  endYear?: number | null;
  description?: string | null;
}

export interface Experience {
  id?: number | null;
  company: string;
  title: string;
  startDate?: string | null;
  endDate?: string | null;
  current: boolean;
  description?: string | null;
}

export interface Preferences {
  preferredIndustries: string[];
  preferredRoles: string[];
  preferredLocations: string[];
}

export interface Profile {
  fullName: string;
  email: string;
  avatarUrl?: string | null;
  phone?: string | null;
  location?: string | null;
  headline?: string | null;
  careerObjective?: string | null;
  employmentStatus?: EmploymentStatus | null;
  yearsOfExperience?: number | null;
  skills: string[];
  education: Education[];
  experience: Experience[];
  preferences: Preferences;
  completionPercentage: number;
}

export interface ExtractedInfo {
  email?: string | null;
  phone?: string | null;
  detectedSections: string[];
  detectedSkills: string[];
  wordCount: number;
}

export interface Resume {
  id: number;
  title: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  source: 'UPLOAD' | 'BUILT';
  createdAt: string;
  latestScore?: number | null;
  extractedInfo?: ExtractedInfo | null;
  extractedText?: string | null;
}

export interface ResumeAnalysis {
  id: number;
  resumeId: number;
  overallScore: number;
  sectionScores: Record<string, number>;
  strengths: string[];
  weaknesses: string[];
  missingInformation: string[];
  recommendations: string[];
  languageIssues: string[];
  summary: string;
  aiPowered: boolean;
  createdAt: string;
}

export type ApplicationStatus =
  | 'SAVED' | 'APPLIED' | 'UNDER_REVIEW' | 'SHORTLISTED' | 'INTERVIEW_SCHEDULED'
  | 'ASSESSMENT' | 'OFFER_RECEIVED' | 'REJECTED' | 'WITHDRAWN';

export interface Application {
  id: number;
  jobId?: number | null;
  jobTitle: string;
  company: string;
  applicationDate?: string | null;
  status: ApplicationStatus;
  applicationUrl?: string | null;
  contactPerson?: string | null;
  interviewDate?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApplicationInput {
  jobId?: number | null;
  jobTitle?: string;
  company?: string;
  applicationDate?: string | null;
  status?: ApplicationStatus;
  applicationUrl?: string | null;
  contactPerson?: string | null;
  interviewDate?: string | null;
  notes?: string | null;
}

export interface ApplicationHistory {
  id: number;
  fromStatus?: string | null;
  toStatus: string;
  note?: string | null;
  changedAt: string;
}

export interface AppNotification {
  id: number;
  type: string;
  title: string;
  message: string;
  link?: string | null;
  read: boolean;
  createdAt: string;
}

export interface Conversation {
  id: number;
  title: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: number | string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  aiPowered?: boolean;
  createdAt?: string;
}

export interface AssistantReply {
  conversationId: number;
  title: string;
  reply: string;
  aiPowered: boolean;
  createdAt: string;
}

export interface Dashboard {
  fullName: string;
  profileCompletion: number;
  savedJobs: number;
  applications: number;
  interviews: number;
  rejected: number;
  offers: number;
  statusBreakdown: Record<string, number>;
  recommendedJobs: Job[];
  recentJobs: Job[];
  recentActivities: { jobTitle: string; company: string; fromStatus?: string | null; toStatus: string; note?: string | null; at: string }[];
  upcomingInterviews: { applicationId: number; jobTitle: string; company: string; interviewDate: string }[];
  resumeSummary?: { resumeId: number; resumeTitle: string; overallScore: number; summary: string; topWeaknesses: string[]; aiPowered: boolean } | null;
  suggestions: string[];
  unreadNotifications: number;
  aiEnabled: boolean;
}

export interface JobFilters {
  q?: string;
  location?: string;
  industry?: string;
  employmentType?: string;
  experienceLevel?: string;
  workArrangement?: string;
  minSalary?: string;
  maxSalary?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ResumeBuildInput {
  title: string;
  template: 'classic' | 'modern';
  personal: { fullName: string; email?: string; phone?: string; location?: string; headline?: string; links?: string };
  summary?: string;
  experience: { company: string; title: string; startDate?: string; endDate?: string; current: boolean; description?: string }[];
  education: { institution: string; degree?: string; fieldOfStudy?: string; period?: string; description?: string }[];
  skills: string[];
  projects: { name: string; description?: string; url?: string }[];
  certifications: { name: string; issuer?: string; year?: string }[];
  achievements: string[];
}
