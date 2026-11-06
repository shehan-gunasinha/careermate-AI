package com.careermate.jobs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    Optional<Job> findBySourceAndExternalId(String source, String externalId);

    boolean existsByFingerprint(String fingerprint);

    Page<Job> findByActiveTrue(Pageable pageable);

    long countByActiveTrue();

    long countByDemoFalseAndActiveTrue();

    @Modifying
    @Query("update Job j set j.active = false where j.active = true and j.deadline is not null and j.deadline < :today")
    int deactivatePastDeadline(@Param("today") LocalDate today);

    @Modifying
    @Query("update Job j set j.active = false where j.active = true and j.source in :sources and j.lastSyncedAt < :cutoff")
    int deactivateStaleExternal(@Param("sources") Collection<String> sources, @Param("cutoff") Instant cutoff);

    @Query("select j from Job j where j.active = true and (j.deadline is null or j.deadline >= :today) order by j.postedDate desc, j.id desc")
    List<Job> findOpenJobs(@Param("today") LocalDate today, Pageable pageable);

    @Query("select j from Job j where j.active = true and j.deadline between :from and :to")
    List<Job> findWithDeadlineBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
