package com.careermate.notifications;

import com.careermate.applications.Application;
import com.careermate.applications.ApplicationRepository;
import com.careermate.applications.ApplicationStatus;
import com.careermate.jobs.Job;
import com.careermate.jobs.JobDtos.JobDto;
import com.careermate.jobs.JobRepository;
import com.careermate.jobs.JobService;
import com.careermate.jobs.SavedJob;
import com.careermate.jobs.SavedJobRepository;
import com.careermate.resume.ResumeAnalysisRepository;
import com.careermate.resume.ResumeRepository;
import com.careermate.user.Role;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Generates in-app reminders. Every notification carries a dedupe key so reruns never create duplicates. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    private static final Set<ApplicationStatus> CLOSED = Set.of(ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final NotificationService notifications;
    private final JobService jobService;

    @Scheduled(cron = "${app.notifications.cron}")
    @Transactional
    public void generateReminders() {
        interviewReminders();
        deadlineReminders();
        resumeReminders();
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void dailyRecommendations() {
        forEachJobSeeker(user -> {
            try {
                var page = jobService.recommended(user, 0, 1);
                if (!page.content().isEmpty()) {
                    JobDto top = page.content().get(0);
                    boolean fresh = top.postedDate() != null && !top.postedDate().isBefore(LocalDate.now().minusDays(7));
                    if (top.matchScore() != null && top.matchScore() >= 70 && fresh) {
                        notifications.create(user, NotificationType.JOB_RECOMMENDATION, "New job match: " + top.title(),
                                top.title() + " at " + top.companyName() + " is a " + top.matchScore() + "% match for your profile.",
                                "/jobs/" + top.id(), "rec-" + top.id());
                    }
                }
            } catch (Exception e) {
                log.debug("Recommendation notification skipped for user {}: {}", user.getId(), e.getMessage());
            }
        });
    }

    void interviewReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Application a : applicationRepository.findByInterviewDateBetween(now, now.plusHours(48))) {
            if (CLOSED.contains(a.getStatus())) {
                continue;
            }
            notifications.create(a.getUser(), NotificationType.INTERVIEW_REMINDER, "Upcoming interview",
                    "Interview for " + a.getJobTitle() + " at " + a.getCompany() + " on "
                            + a.getInterviewDate().format(DateTimeFormatter.ofPattern("EEE d MMM, HH:mm")) + ".",
                    "/applications", "interview-" + a.getId() + "-" + a.getInterviewDate());
        }
    }

    void deadlineReminders() {
        LocalDate today = LocalDate.now();
        List<Job> closing = jobRepository.findWithDeadlineBetween(today, today.plusDays(3));
        if (closing.isEmpty()) {
            return;
        }
        List<Long> ids = closing.stream().map(Job::getId).toList();
        for (SavedJob s : savedJobRepository.findByJobIdIn(ids)) {
            Job j = s.getJob();
            notifications.create(s.getUser(), NotificationType.DEADLINE_REMINDER, "Application deadline approaching",
                    "Applications for " + j.getTitle() + " at " + j.getCompanyName() + " close on " + j.getDeadline() + ".",
                    "/jobs/" + j.getId(), "deadline-" + j.getId());
        }
        for (Application a : applicationRepository.findByJobIdIn(ids)) {
            if (a.getStatus() == ApplicationStatus.SAVED && a.getJob() != null) {
                notifications.create(a.getUser(), NotificationType.DEADLINE_REMINDER, "Application deadline approaching",
                        "You saved " + a.getJobTitle() + " at " + a.getCompany() + " but have not applied yet. Deadline: " + a.getJob().getDeadline() + ".",
                        "/applications", "deadline-" + a.getJob().getId());
            }
        }
    }

    void resumeReminders() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        forEachJobSeeker(user -> {
            if (user.getCreatedAt() == null || user.getCreatedAt().isAfter(cutoff)) {
                return;
            }
            var latest = resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
            if (latest == null) {
                notifications.create(user, NotificationType.RESUME_REMINDER, "Upload your resume",
                        "Upload your resume to get an analysis and better job matches.", "/resumes", "resume-upload");
                return;
            }
            var analysis = analysisRepository.findFirstByResumeIdOrderByCreatedAtDescIdDesc(latest.getId()).orElse(null);
            if (analysis == null) {
                notifications.create(user, NotificationType.RESUME_REMINDER, "Analyse your resume",
                        "Your resume has not been analysed yet. Get your score and recommendations.",
                        "/resumes/" + latest.getId() + "/analysis", "resume-analyze-" + latest.getId());
            } else if (analysis.getOverallScore() < 60) {
                notifications.create(user, NotificationType.RESUME_REMINDER, "Improve your resume",
                        "Your latest resume score is " + analysis.getOverallScore() + "/100. Review the recommendations.",
                        "/resumes/" + latest.getId() + "/analysis", "resume-improve-" + analysis.getId());
            }
        });
    }

    private void forEachJobSeeker(java.util.function.Consumer<User> action) {
        for (int p = 0; p < 50; p++) {
            Page<User> page = userRepository.findAll(PageRequest.of(p, 200));
            page.getContent().stream().filter(u -> u.isEnabled() && u.getRole() == Role.JOB_SEEKER).forEach(action);
            if (!page.hasNext()) {
                break;
            }
        }
    }
}
