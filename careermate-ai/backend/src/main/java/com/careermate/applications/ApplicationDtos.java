package com.careermate.applications;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ApplicationDtos {
    private ApplicationDtos() {
    }

    public record ApplicationRequest(Long jobId,
                                     @Size(max = 200) String jobTitle,
                                     @Size(max = 200) String company,
                                     LocalDate applicationDate,
                                     ApplicationStatus status,
                                     @Size(max = 1000) String applicationUrl,
                                     @Size(max = 200) String contactPerson,
                                     LocalDateTime interviewDate,
                                     @Size(max = 4000) String notes) {
    }

    public record StatusUpdateRequest(@NotNull(message = "Status is required") ApplicationStatus status,
                                      @Size(max = 1000) String note) {
    }

    public record ApplicationDto(Long id, Long jobId, String jobTitle, String company, LocalDate applicationDate,
                                 ApplicationStatus status, String applicationUrl, String contactPerson,
                                 LocalDateTime interviewDate, String notes, Instant createdAt, Instant updatedAt) {
    }

    public record HistoryDto(Long id, String fromStatus, String toStatus, String note, Instant changedAt) {
    }
}
