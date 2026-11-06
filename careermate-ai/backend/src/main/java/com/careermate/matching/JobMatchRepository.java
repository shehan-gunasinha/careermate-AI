package com.careermate.matching;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {
    Optional<JobMatch> findByUserIdAndJobId(Long userId, Long jobId);
}
