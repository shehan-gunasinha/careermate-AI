package com.careermate.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class ProfileDtos {
    private ProfileDtos() {
    }

    public record EducationDto(Long id,
                               @NotBlank(message = "Institution is required") @Size(max = 200) String institution,
                               @Size(max = 200) String degree,
                               @Size(max = 200) String fieldOfStudy,
                               @Min(1950) @Max(2100) Integer startYear,
                               @Min(1950) @Max(2100) Integer endYear,
                               @Size(max = 2000) String description) {
    }

    public record ExperienceDto(Long id,
                                @NotBlank(message = "Company is required") @Size(max = 200) String company,
                                @NotBlank(message = "Job title is required") @Size(max = 200) String title,
                                LocalDate startDate,
                                LocalDate endDate,
                                boolean current,
                                @Size(max = 4000) String description) {
    }

    public record PreferencesDto(@Size(max = 30) List<@Size(max = 100) String> preferredIndustries,
                                 @Size(max = 30) List<@Size(max = 100) String> preferredRoles,
                                 @Size(max = 30) List<@Size(max = 100) String> preferredLocations) {
    }

    public record ProfileDto(String fullName, String email, String phone, String location, String headline,
                             String careerObjective, EmploymentStatus employmentStatus, Integer yearsOfExperience,
                             List<String> skills, List<EducationDto> education, List<ExperienceDto> experience,
                             PreferencesDto preferences, int completionPercentage) {
    }

    public record ProfileUpdateRequest(@NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
                                       @Size(max = 30) String phone,
                                       @Size(max = 150) String location,
                                       @Size(max = 200) String headline,
                                       @Size(max = 3000) String careerObjective,
                                       EmploymentStatus employmentStatus,
                                       @Min(0) @Max(60) Integer yearsOfExperience,
                                       @Size(max = 100) List<@Size(max = 80) String> skills,
                                       @Valid @Size(max = 30) List<EducationDto> education,
                                       @Valid @Size(max = 30) List<ExperienceDto> experience) {
    }
}
