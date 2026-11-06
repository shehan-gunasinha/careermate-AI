package com.careermate.jobs;

import com.careermate.common.response.ApiResponse;
import com.careermate.jobs.JobDtos.ImportResult;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.careermate.jobs.JobDtos.JobDto;
import com.careermate.jobs.JobDtos.SyncResult;
import com.careermate.jobs.ingestion.JobIngestionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administrative job management (ROLE_ADMIN only; enforced in SecurityConfig and by method security). */
@RestController
@RequestMapping("/api/v1/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminJobController {
    private final JobService jobService;
    private final JobIngestionService ingestionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobDto> create(@Valid @RequestBody JobCreateRequest req) {
        return ApiResponse.ok("Job created", jobService.create(req, "MANUAL"));
    }

    @PostMapping("/import")
    public ApiResponse<ImportResult> importJobs(@RequestBody List<@Valid JobCreateRequest> jobs) {
        return ApiResponse.ok(jobService.importJobs(jobs));
    }

    @PostMapping("/sync")
    public ApiResponse<List<SyncResult>> sync() {
        return ApiResponse.ok("Synchronisation finished", ingestionService.syncAll());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        jobService.deactivate(id);
        return ApiResponse.ok("Job deactivated", null);
    }
}
