package com.careermate.dashboard;

import com.careermate.ai.GeminiKeyService;
import com.careermate.applications.ApplicationHistoryRepository;
import com.careermate.applications.ApplicationRepository;
import com.careermate.applications.ApplicationStatus;
import com.careermate.common.util.Json;
import com.careermate.dashboard.DashboardDtos.ActivityDto;
import com.careermate.dashboard.DashboardDtos.DashboardDto;
import com.careermate.dashboard.DashboardDtos.InterviewDto;
import com.careermate.dashboard.DashboardDtos.ResumeSummaryDto;
import com.careermate.jobs.JobDtos.JobDto;
import com.careermate.jobs.JobService;
import com.careermate.jobs.SavedJobRepository;
import com.careermate.notifications.NotificationRepository;
import com.careermate.profile.ProfileService;
import com.careermate.resume.Resume;
import com.careermate.resume.ResumeAnalysis;
import com.careermate.resume.ResumeAnalysisRepository;
import com.careermate.resume.ResumeRepository;
import com.careermate.user.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every number on the dashboard is computed from the database at request time. */
@Service
@RequiredArgsConstructor
public class DashboardService {
    private final SavedJobRepository savedJobRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository analysisRepository;
    private final NotificationRepository notificationRepository;
    private final ProfileService profileService;
    private final JobService jobService;
    private final GeminiKeyService geminiKeys;

    @Transactional(readOnly = true)
    public DashboardDto build(User user) {
        Long uid = user.getId();
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (Object[] row : applicationRepository.countByStatus(uid)) {
            counts.put((ApplicationStatus) row[0], (Long) row[1]);
        }
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            breakdown.put(s.name(), counts.getOrDefault(s, 0L));
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        int completion = profileService.completionPercentage(user);

        List<JobDto> recommended = jobService.recommended(user, 0, 5).content();
        List<JobDto> recent = jobService.latest(user, 5);

        List<ActivityDto> activities = historyRepository.findByApplicationUserIdOrderByChangedAtDescIdDesc(uid, PageRequest.of(0, 8)).stream()
                .map(h -> new ActivityDto(h.getApplication().getJobTitle(), h.getApplication().getCompany(), h.getFromStatus(),
                        h.getToStatus(), h.getNote(), h.getChangedAt())).toList();

        List<InterviewDto> interviews = applicationRepository
                .findByUserIdAndInterviewDateAfterOrderByInterviewDateAsc(uid, LocalDateTime.now(), PageRequest.of(0, 5)).stream()
                .map(a -> new InterviewDto(a.getId(), a.getJobTitle(), a.getCompany(), a.getInterviewDate())).toList();

        Resume latest = resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(uid).orElse(null);
        ResumeAnalysis analysis = analysisRepository.findFirstByResumeUserIdOrderByCreatedAtDescIdDesc(uid).orElse(null);
        ResumeSummaryDto resumeSummary = analysis == null ? null : new ResumeSummaryDto(analysis.getResume().getId(),
                analysis.getResume().getTitle(), analysis.getOverallScore(), analysis.getSummary(),
                Json.readStringList(analysis.getWeaknesses()).stream().limit(3).toList(), analysis.isAiPowered());

        List<String> suggestions = new ArrayList<>();
        if (completion < 100) suggestions.add("Your profile is " + completion + "% complete. Fill in the missing sections to get better job matches.");
        if (latest == null) suggestions.add("Upload your resume to unlock resume analysis and more accurate job matching.");
        else if (analysis == null) suggestions.add("Run an analysis on your resume to see your score and improvement tips.");
        else if (analysis.getOverallScore() < 70) suggestions.add("Your latest resume score is " + analysis.getOverallScore() + "/100. Follow the recommendations to improve it.");
        if (total == 0) suggestions.add("Start tracking your applications so you never miss a follow-up or interview.");
        if (!interviews.isEmpty()) suggestions.add("You have an upcoming interview. Ask CareerMate AI to help you prepare.");
        if (savedJobs(uid) > 0 && counts.getOrDefault(ApplicationStatus.APPLIED, 0L) == 0) suggestions.add("You have saved jobs but no applications yet. Pick one and apply this week.");
        if (suggestions.isEmpty()) suggestions.add("You are on track. Keep applying and tailor your resume to each role.");

        return new DashboardDto(user.getFullName(), completion, savedJobs(uid), total,
                counts.getOrDefault(ApplicationStatus.INTERVIEW_SCHEDULED, 0L), counts.getOrDefault(ApplicationStatus.REJECTED, 0L),
                counts.getOrDefault(ApplicationStatus.OFFER_RECEIVED, 0L), breakdown, recommended, recent, activities,
                interviews, resumeSummary, suggestions, notificationRepository.countByUserIdAndReadFalse(uid), geminiKeys.isConfigured(user));
    }

    private long savedJobs(Long uid) {
        return savedJobRepository.countByUserId(uid);
    }
}
