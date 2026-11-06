package com.careermate.jobs;

import com.careermate.common.response.ApiResponse;
import com.careermate.common.response.PageResponse;
import com.careermate.jobs.JobDtos.JobDto;
import com.careermate.jobs.JobDtos.JobSearchCriteria;
import com.careermate.matching.MatchResult;
import com.careermate.user.User;
import com.careermate.common.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {
    private static final Set<String> SORTABLE = Set.of("postedDate", "title", "companyName", "location", "salaryMin",
            "salaryMax", "deadline", "createdAt", "experienceLevel", "employmentType");

    private final JobService jobService;

    @GetMapping
    public ApiResponse<PageResponse<JobDto>> search(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) WorkArrangement workArrangement,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @PageableDefault(size = 12, sort = "postedDate", direction = Sort.Direction.DESC) Pageable pageable) {
        pageable.getSort().forEach(o -> {
            if (!SORTABLE.contains(o.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + o.getProperty() + "'");
            }
        });
        JobSearchCriteria c = new JobSearchCriteria(q, title, company, industry, location, employmentType,
                experienceLevel, workArrangement, minSalary, maxSalary, skill);
        return ApiResponse.ok(jobService.search(user, c, pageable));
    }

    @GetMapping("/recommended")
    public ApiResponse<PageResponse<JobDto>> recommended(@AuthenticationPrincipal User user,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(jobService.recommended(user, page, size));
    }

    @GetMapping("/saved")
    public ApiResponse<PageResponse<JobDto>> saved(@AuthenticationPrincipal User user,
                                                   @PageableDefault(size = 12) Pageable pageable) {
        return ApiResponse.ok(jobService.saved(user, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobDto> get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(jobService.get(user, id));
    }

    @GetMapping("/{id}/match")
    public ApiResponse<MatchResult> match(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(jobService.match(user, id));
    }

    @PostMapping("/{id}/save")
    public ApiResponse<Void> save(@AuthenticationPrincipal User user, @PathVariable Long id) {
        jobService.save(user, id);
        return ApiResponse.ok("Job saved", null);
    }

    @DeleteMapping("/{id}/save")
    public ApiResponse<Void> unsave(@AuthenticationPrincipal User user, @PathVariable Long id) {
        jobService.unsave(user, id);
        return ApiResponse.ok("Job removed from saved jobs", null);
    }
}
