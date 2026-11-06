package com.careermate.resume;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    Optional<Resume> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserId(Long userId);

    long countByUserId(Long userId);
}
