package com.careermate.resume;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ResumeDtos {
    private ResumeDtos() {
    }

    public record ExtractedInfo(String email, String phone, List<String> detectedSections, List<String> detectedSkills,
                                int wordCount) {
    }

    public record ResumeDto(Long id, String title, String originalFilename, String contentType, long sizeBytes,
                            ResumeSource source, Instant createdAt, Integer latestScore, ExtractedInfo extractedInfo,
                            String extractedText) {
    }

    public record AnalysisDto(Long id, Long resumeId, int overallScore, Map<String, Integer> sectionScores,
                              List<String> strengths, List<String> weaknesses, List<String> missingInformation,
                              List<String> recommendations, List<String> languageIssues, String summary,
                              boolean aiPowered, Instant createdAt) {
    }

    public record PersonalDto(@NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
                              @Size(max = 255) String email, @Size(max = 40) String phone,
                              @Size(max = 150) String location, @Size(max = 200) String headline,
                              @Size(max = 300) String links) {
    }

    public record ExperienceItem(@NotBlank @Size(max = 200) String company, @NotBlank @Size(max = 200) String title,
                                 @Size(max = 40) String startDate, @Size(max = 40) String endDate, boolean current,
                                 @Size(max = 3000) String description) {
    }

    public record EducationItem(@NotBlank @Size(max = 200) String institution, @Size(max = 200) String degree,
                                @Size(max = 200) String fieldOfStudy, @Size(max = 40) String period,
                                @Size(max = 1000) String description) {
    }

    public record ProjectItem(@NotBlank @Size(max = 200) String name, @Size(max = 2000) String description,
                              @Size(max = 300) String url) {
    }

    public record CertificationItem(@NotBlank @Size(max = 200) String name, @Size(max = 200) String issuer,
                                    @Size(max = 20) String year) {
    }

    public record ResumeBuildRequest(@NotBlank(message = "Resume title is required") @Size(max = 100) String title,
                                     @Size(max = 20) String template,
                                     @NotNull @Valid PersonalDto personal,
                                     @Size(max = 2000) String summary,
                                     @Valid @Size(max = 20) List<ExperienceItem> experience,
                                     @Valid @Size(max = 20) List<EducationItem> education,
                                     @Size(max = 60) List<@Size(max = 80) String> skills,
                                     @Valid @Size(max = 20) List<ProjectItem> projects,
                                     @Valid @Size(max = 20) List<CertificationItem> certifications,
                                     @Size(max = 30) List<@Size(max = 500) String> achievements) {
    }

    public record SuggestRequest(@NotBlank @Size(max = 40) String section,
                                 @NotBlank @Size(max = 4000) String content,
                                 @Size(max = 200) String targetRole) {
    }

    public record SuggestResponse(String suggestion, boolean aiPowered) {
    }

    public record DownloadFile(byte[] bytes, String filename, String contentType) {
    }
}
