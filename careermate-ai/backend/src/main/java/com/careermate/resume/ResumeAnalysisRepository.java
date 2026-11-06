package com.careermate.resume;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {
    Optional<ResumeAnalysis> findFirstByResumeIdOrderByCreatedAtDescIdDesc(Long resumeId);

    List<ResumeAnalysis> findByResumeIdOrderByCreatedAtDescIdDesc(Long resumeId);

    Optional<ResumeAnalysis> findFirstByResumeUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @Modifying
    @Query("delete from ResumeAnalysis a where a.resume.id = :resumeId")
    void deleteByResumeId(@Param("resumeId") Long resumeId);
}
