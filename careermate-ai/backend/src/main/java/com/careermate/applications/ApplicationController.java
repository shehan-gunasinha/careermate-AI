package com.careermate.applications;

import com.careermate.applications.ApplicationDtos.ApplicationDto;
import com.careermate.applications.ApplicationDtos.ApplicationRequest;
import com.careermate.applications.ApplicationDtos.HistoryDto;
import com.careermate.applications.ApplicationDtos.StatusUpdateRequest;
import com.careermate.common.response.ApiResponse;
import com.careermate.common.response.PageResponse;
import com.careermate.user.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService service;

    @GetMapping
    public ApiResponse<PageResponse<ApplicationDto>> list(@AuthenticationPrincipal User user,
                                                          @RequestParam(required = false) ApplicationStatus status,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(defaultValue = "updatedAt") String sortBy,
                                                          @RequestParam(defaultValue = "false") boolean asc) {
        return ApiResponse.ok(service.search(user, status, q, page, size, sortBy, asc));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ApplicationDto> create(@AuthenticationPrincipal User user, @Valid @RequestBody ApplicationRequest req) {
        return ApiResponse.ok("Application added", service.create(user, req));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationDto> get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(service.get(user, id));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<HistoryDto>> history(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(service.history(user, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApplicationDto> update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                              @Valid @RequestBody ApplicationRequest req) {
        return ApiResponse.ok("Application updated", service.update(user, id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ApplicationDto> status(@AuthenticationPrincipal User user, @PathVariable Long id,
                                              @Valid @RequestBody StatusUpdateRequest req) {
        return ApiResponse.ok("Status updated", service.changeStatus(user, id, req.status(), req.note()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user, id);
        return ApiResponse.ok("Application deleted", null);
    }
}
