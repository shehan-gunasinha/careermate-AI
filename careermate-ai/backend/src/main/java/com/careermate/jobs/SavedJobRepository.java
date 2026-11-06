package com.careermate.jobs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    Optional<SavedJob> findByUserIdAndJobId(Long userId, Long jobId);

    Page<SavedJob> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    List<SavedJob> findByJobIdIn(Collection<Long> jobIds);

    @Query("select s.job.id from SavedJob s where s.user.id = :userId")
    Set<Long> findJobIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndJobId(Long userId, Long jobId);
}
