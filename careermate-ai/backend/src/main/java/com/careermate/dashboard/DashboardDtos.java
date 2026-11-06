package com.careermate.dashboard;

import com.careermate.jobs.JobDtos.JobDto;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record ActivityDto(String jobTitle, String company, String fromStatus, String toStatus, String note, Instant at) {
    }

    public record InterviewDto(Long applicationId, String jobTitle, String company, LocalDateTime interviewDate) {
    }

    public record ResumeSummaryDto(Long resumeId, String resumeTitle, Integer overallScore, String summary,
                                   List<String> topWeaknesses, boolean aiPowered) {
    }

    public record DashboardDto(String fullName, int profileCompletion, long savedJobs, long applications, long interviews,
                               long rejected, long offers, Map<String, Long> statusBreakdown,
                               List<JobDto> recommendedJobs, List<JobDto> recentJobs, List<ActivityDto> recentActivities,
                               List<InterviewDto> upcomingInterviews, ResumeSummaryDto resumeSummary,
                               List<String> suggestions, long unreadNotifications, boolean aiEnabled) {
    }
}
