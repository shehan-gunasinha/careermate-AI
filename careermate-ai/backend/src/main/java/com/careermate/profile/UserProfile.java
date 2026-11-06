package com.careermate.profile;

import com.careermate.common.util.BaseEntity;
import com.careermate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String location;

    @Column(length = 200)
    private String headline;

    @Column(name = "career_objective", columnDefinition = "TEXT")
    private String careerObjective;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 30)
    private EmploymentStatus employmentStatus;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "avatar_data", columnDefinition = "bytea")
    private byte[] avatarData;

    @Column(name = "avatar_content_type", length = 100)
    private String avatarContentType;

    @Column(name = "avatar_updated_at")
    private Instant avatarUpdatedAt;
}
