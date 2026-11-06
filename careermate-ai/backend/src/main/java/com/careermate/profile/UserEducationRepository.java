package com.careermate.profile;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserEducationRepository extends JpaRepository<UserEducation, Long> {
    List<UserEducation> findByUserIdOrderByIdAsc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserEducation e where e.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
