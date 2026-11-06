package com.careermate.applications;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, Long> {
    List<ApplicationHistory> findByApplicationIdOrderByChangedAtDescIdDesc(Long applicationId);

    List<ApplicationHistory> findByApplicationUserIdOrderByChangedAtDescIdDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("delete from ApplicationHistory h where h.application.id = :applicationId")
    void deleteByApplicationId(@Param("applicationId") Long applicationId);
}
