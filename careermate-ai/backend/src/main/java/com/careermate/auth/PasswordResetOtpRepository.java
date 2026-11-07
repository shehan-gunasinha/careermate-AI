package com.careermate.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PasswordResetOtp> findTopByUserIdAndConsumedFalseOrderByCreatedAtDesc(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetOtp o set o.consumed = true where o.user.id = :userId and o.consumed = false")
    int invalidateActive(@Param("userId") Long userId);
}
