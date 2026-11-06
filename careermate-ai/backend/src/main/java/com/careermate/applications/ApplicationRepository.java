package com.careermate.applications;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    Optional<Application> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("select a.status, count(a) from Application a where a.user.id = :userId group by a.status")
    List<Object[]> countByStatus(@Param("userId") Long userId);

    List<Application> findByUserIdAndInterviewDateAfterOrderByInterviewDateAsc(Long userId, LocalDateTime after, Pageable pageable);

    List<Application> findByInterviewDateBetween(LocalDateTime from, LocalDateTime to);

    List<Application> findByJobIdIn(Collection<Long> jobIds);
}
