package com.careermate.jobs;

import com.careermate.common.util.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_logo_url", length = 1000)
    private String companyLogoUrl;

    @Column(name = "company_description", length = 2000)
    private String companyDescription;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 100)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_arrangement", nullable = false, length = 30)
    private WorkArrangement workArrangement;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 30)
    private ExperienceLevel experienceLevel;

    @Column(name = "salary_min", precision = 14, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 14, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", length = 10)
    private String salaryCurrency;

    @Column(name = "short_description", length = 600)
    private String shortDescription;

    @Column(nullable = false, length = 20000)
    private String description;

    @Column(length = 8000)
    private String responsibilities;

    @Column(name = "required_qualifications", length = 8000)
    private String requiredQualifications;

    @Column(name = "preferred_qualifications", length = 8000)
    private String preferredQualifications;

    @Column(name = "posted_date")
    private LocalDate postedDate;

    private LocalDate deadline;

    @Column(name = "application_url", length = 1000)
    private String applicationUrl;

    @Column(nullable = false, length = 40)
    private String source = "MANUAL";

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(nullable = false, unique = true, length = 64)
    private String fingerprint;

    @Column(nullable = false)
    private boolean demo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkill> skills = new ArrayList<>();
}
