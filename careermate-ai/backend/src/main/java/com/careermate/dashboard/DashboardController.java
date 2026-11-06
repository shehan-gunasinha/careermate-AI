package com.careermate.dashboard;

import com.careermate.common.response.ApiResponse;
import com.careermate.dashboard.DashboardDtos.DashboardDto;
import com.careermate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping
    public ApiResponse<DashboardDto> get(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.build(user));
    }
}
