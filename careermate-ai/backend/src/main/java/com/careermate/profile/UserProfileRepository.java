package com.careermate.profile;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    @Query("select p.avatarUpdatedAt from UserProfile p where p.user.id = :userId")
    Optional<Instant> findAvatarUpdatedAtByUserId(@Param("userId") Long userId);
}
