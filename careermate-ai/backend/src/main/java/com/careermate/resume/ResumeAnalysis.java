package com.careermate.resume;

import com.careermate.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One analysis run. Lists and the section score map are stored as JSON text. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resume_analysis")
public class ResumeAnalysis extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "section_scores", columnDefinition = "TEXT")
    private String sectionScores;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "missing_information", columnDefinition = "TEXT")
    private String missingInformation;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "language_issues", columnDefinition = "TEXT")
    private String languageIssues;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "ai_powered", nullable = false)
    private boolean aiPowered;
}
