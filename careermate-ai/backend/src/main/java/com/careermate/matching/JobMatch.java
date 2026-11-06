package com.careermate.matching;

import com.careermate.common.util.BaseEntity;
import com.careermate.jobs.Job;
import com.careermate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_matches", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
public class JobMatch extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private int score;

    @Column(name = "ai_powered", nullable = false)
    private boolean aiPowered;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
