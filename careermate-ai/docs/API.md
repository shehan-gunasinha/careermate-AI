# CareerMate AI REST API

Base path: `/api/v1`. All responses: `{ "success": bool, "message": string, "data": any, "timestamp": ISO-8601 }`.
Authenticated endpoints need `Authorization: Bearer <jwt>`. Errors: `400` validation (`data` holds field messages),
`401` not authenticated, `403` forbidden, `404` not found (also used for other users' resources), `409` conflict, `413` file too large,
`503` AI unavailable (only returned where no fallback exists).
Interactive documentation: `/swagger-ui.html`.

## Auth
| Method | Path | Notes |
|---|---|---|
| POST | `/auth/register` | `fullName, email, password, confirmPassword` -> token + user (201) |
| POST | `/auth/login` | `email, password` |
| POST | `/auth/forgot-password` | `email` - emails a 6-digit reset code; same response for unknown emails |
| POST | `/auth/reset-password` | `email, otp, newPassword, confirmPassword` |
| GET | `/auth/me` | current user |
| POST | `/auth/logout` | revokes the presented token |
| PUT | `/auth/password` | `currentPassword, newPassword, confirmPassword` |

## Dashboard
`GET /dashboard` - counts, status breakdown, profile completion, recommended + recent jobs, recent activity, upcoming interviews, resume summary, suggestions.

## Profile
| Method | Path | Notes |
|---|---|---|
| GET | `/profile` | includes avatar, skills, education, experience, preferences, completion % |
| PUT | `/profile` | omitted lists are left unchanged; provided lists replace existing ones |
| PUT | `/profile/preferences` | `preferredIndustries, preferredRoles, preferredLocations` |
| POST | `/profile/avatar` | multipart `file` (PNG/JPG/WEBP, max 2 MB) |
| DELETE | `/profile/avatar` | removes the current picture |
| GET | `/profile/avatar/{userId}` | public image endpoint used by `<img>` (cache-friendly) |

## Jobs
| Method | Path | Notes |
|---|---|---|
| GET | `/jobs` | `q, title, company, industry, location, skill, employmentType, experienceLevel, workArrangement, minSalary, maxSalary, page, size, sort` |
| GET | `/jobs/{id}` | detail incl. `saved` |
| GET | `/jobs/recommended` | `page, size`; ranked with `matchScore` |
| GET | `/jobs/{id}/match` | score, matching/missing skills, strengths, improvements, recommendations, `aiPowered` |
| POST / DELETE | `/jobs/{id}/save` | idempotent |
| GET | `/jobs/saved` | paged |

Enums: `employmentType` FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT, FREELANCE, TEMPORARY; `experienceLevel` ENTRY, JUNIOR, MID,
SENIOR, EXECUTIVE; `workArrangement` REMOTE, HYBRID, ON_SITE. Sortable: postedDate, title, companyName, location, salaryMin,
salaryMax, deadline, createdAt, experienceLevel, employmentType.

## Resumes
| Method | Path | Notes |
|---|---|---|
| POST | `/resumes/upload` | multipart `file` (PDF/DOCX, max 5 MB), optional `title` |
| GET | `/resumes`, `/resumes/{id}` | detail includes extracted info and text |
| DELETE | `/resumes/{id}` | removes file and analyses |
| POST | `/resumes/{id}/analyze` | creates a new analysis (history is kept) |
| GET | `/resumes/{id}/analysis` | latest; 404 if none |
| GET | `/resumes/{id}/analyses` | history |
| POST | `/resumes/build` | resume builder form -> stored PDF |
| POST | `/resumes/suggest` | `section, content, targetRole?` -> editable improved text |
| GET | `/resumes/{id}/download` | original or built file |

## Applications
Statuses: SAVED, APPLIED, UNDER_REVIEW, SHORTLISTED, INTERVIEW_SCHEDULED, ASSESSMENT, OFFER_RECEIVED, REJECTED, WITHDRAWN.

| Method | Path | Notes |
|---|---|---|
| GET | `/applications` | `status, q, page, size, sortBy (updatedAt/company/jobTitle/status/applicationDate), asc` |
| POST | `/applications` | `jobId?` or `jobTitle + company`, `status, applicationDate, applicationUrl, contactPerson, interviewDate, notes` |
| GET / PUT / DELETE | `/applications/{id}` | |
| PATCH | `/applications/{id}/status` | `status, note?` (writes history + notification) |
| GET | `/applications/{id}/history` | |

## Career assistant
| Method | Path | Notes |
|---|---|---|
| POST | `/career-assistant` | `message, conversationId?, useProfile?` -> `reply, aiPowered` |
| GET | `/career-assistant/status` | `aiEnabled, mode` |
| GET | `/career-assistant/conversations` | |
| GET | `/career-assistant/conversations/{id}/messages` | |
| DELETE | `/career-assistant/conversations/{id}` | clear a conversation |

## AI key
| Method | Path | Notes |
|---|---|---|
| GET | `/ai/gemini-key` | `configured, hint, serverKeyConfigured, aiEnabled, updatedAt`; never returns the full key |
| PUT | `/ai/gemini-key` | `apiKey` - validates with Google, then encrypts and saves it for the current user |
| DELETE | `/ai/gemini-key` | removes the current user's key; falls back to the shared server key or built-in logic |

## Notifications
`GET /notifications?unreadOnly&page&size`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`.

## Admin (ROLE_ADMIN)
`POST /admin/jobs`, `POST /admin/jobs/import` (array), `POST /admin/jobs/sync`, `DELETE /admin/jobs/{id}` (deactivate).
