package com.careermate.profile;

import com.careermate.common.util.BaseEntity;
import com.careermate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Preference lists are stored as JSON arrays in TEXT columns. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_preferences")
public class UserPreference extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_industries", columnDefinition = "TEXT")
    private String preferredIndustries;

    @Column(name = "preferred_roles", columnDefinition = "TEXT")
    private String preferredRoles;

    @Column(name = "preferred_locations", columnDefinition = "TEXT")
    private String preferredLocations;
}
