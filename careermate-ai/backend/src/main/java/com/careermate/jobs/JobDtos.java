package com.careermate.jobs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class JobDtos {
    private JobDtos() {
    }

    public record JobDto(Long id, String title, String companyName, String companyLogoUrl, String companyDescription,
                         String location, String industry, EmploymentType employmentType,
                         WorkArrangement workArrangement, ExperienceLevel experienceLevel,
                         BigDecimal salaryMin, BigDecimal salaryMax, String salaryCurrency,
                         String shortDescription, String description, String responsibilities,
                         String requiredQualifications, String preferredQualifications,
                         List<String> requiredSkills, List<String> preferredSkills,
                         LocalDate postedDate, LocalDate deadline, String applicationUrl, String source,
                         boolean demo, boolean active, boolean saved, Integer matchScore) {
    }

    public record JobCreateRequest(@NotBlank(message = "Title is required") @Size(max = 200) String title,
                                   @NotBlank(message = "Company name is required") @Size(max = 200) String companyName,
                                   @Size(max = 1000) String companyLogoUrl,
                                   @Size(max = 2000) String companyDescription,
                                   @NotBlank(message = "Location is required") @Size(max = 200) String location,
                                   @Size(max = 100) String industry,
                                   @NotNull(message = "Employment type is required") EmploymentType employmentType,
                                   @NotNull(message = "Work arrangement is required") WorkArrangement workArrangement,
                                   @NotNull(message = "Experience level is required") ExperienceLevel experienceLevel,
                                   @PositiveOrZero BigDecimal salaryMin,
                                   @PositiveOrZero BigDecimal salaryMax,
                                   @Size(max = 10) String salaryCurrency,
                                   @Size(max = 600) String shortDescription,
                                   @NotBlank(message = "Description is required") @Size(max = 20000) String description,
                                   @Size(max = 8000) String responsibilities,
                                   @Size(max = 8000) String requiredQualifications,
                                   @Size(max = 8000) String preferredQualifications,
                                   @Size(max = 40) List<@Size(max = 80) String> requiredSkills,
                                   @Size(max = 40) List<@Size(max = 80) String> preferredSkills,
                                   LocalDate postedDate, LocalDate deadline,
                                   @Size(max = 1000) String applicationUrl,
                                   @Size(max = 40) String source) {
    }

    public record JobSearchCriteria(String q, String title, String company, String industry, String location,
                                    EmploymentType employmentType, ExperienceLevel experienceLevel,
                                    WorkArrangement workArrangement, BigDecimal minSalary, BigDecimal maxSalary,
                                    String skill) {
    }

    public record ImportResult(int created, int duplicates, List<String> errors) {
    }

    public record SyncResult(String source, int fetched, int created, int updated, int duplicates, String error) {
    }
}
