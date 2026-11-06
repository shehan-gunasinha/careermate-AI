package com.careermate.applications;

import com.careermate.common.util.BaseEntity;
import com.careermate.jobs.Job;
import com.careermate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "applications")
public class Application extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "job_title", nullable = false, length = 200)
    private String jobTitle;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(name = "application_date")
    private LocalDate applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.SAVED;

    @Column(name = "application_url", length = 1000)
    private String applicationUrl;

    @Column(name = "contact_person", length = 200)
    private String contactPerson;

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @Column(length = 4000)
    private String notes;
}
