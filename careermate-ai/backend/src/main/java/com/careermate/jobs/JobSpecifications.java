package com.careermate.jobs;

import com.careermate.jobs.JobDtos.JobSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {
    private JobSpecifications() {
    }

    public static Specification<Job> from(JobSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("active")));
            p.add(cb.or(cb.isNull(root.get("deadline")), cb.greaterThanOrEqualTo(root.<LocalDate>get("deadline"), LocalDate.now())));

            if (has(c.q())) {
                String like = like(c.q());
                Subquery<Long> sq = query.subquery(Long.class);
                Root<JobSkill> js = sq.from(JobSkill.class);
                sq.select(js.<Long>get("id")).where(cb.equal(js.get("job"), root), cb.like(cb.lower(js.get("skill")), like));
                p.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(root.get("shortDescription")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("industry")), like),
                        cb.exists(sq)));
            }
            if (has(c.title())) {
                p.add(cb.like(cb.lower(root.get("title")), like(c.title())));
            }
            if (has(c.company())) {
                p.add(cb.like(cb.lower(root.get("companyName")), like(c.company())));
            }
            if (has(c.industry())) {
                p.add(cb.like(cb.lower(root.get("industry")), like(c.industry())));
            }
            if (has(c.location())) {
                p.add(cb.like(cb.lower(root.get("location")), like(c.location())));
            }
            if (has(c.skill())) {
                Subquery<Long> sq = query.subquery(Long.class);
                Root<JobSkill> js = sq.from(JobSkill.class);
                sq.select(js.<Long>get("id")).where(cb.equal(js.get("job"), root), cb.like(cb.lower(js.get("skill")), like(c.skill())));
                p.add(cb.exists(sq));
            }
            if (c.employmentType() != null) {
                p.add(cb.equal(root.get("employmentType"), c.employmentType()));
            }
            if (c.experienceLevel() != null) {
                p.add(cb.equal(root.get("experienceLevel"), c.experienceLevel()));
            }
            if (c.workArrangement() != null) {
                p.add(cb.equal(root.get("workArrangement"), c.workArrangement()));
            }
            if (c.minSalary() != null) {
                p.add(cb.or(cb.isNull(root.get("salaryMax")), cb.greaterThanOrEqualTo(root.get("salaryMax"), c.minSalary())));
            }
            if (c.maxSalary() != null) {
                p.add(cb.or(cb.isNull(root.get("salaryMin")), cb.lessThanOrEqualTo(root.get("salaryMin"), c.maxSalary())));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private static String like(String s) {
        String escaped = s.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
