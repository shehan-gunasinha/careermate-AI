package com.careermate.notifications;

import com.careermate.common.response.ApiResponse;
import com.careermate.common.response.PageResponse;
import com.careermate.user.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public ApiResponse<PageResponse<NotificationDto>> list(@AuthenticationPrincipal User user,
                                                           @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(user, unreadOnly, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(Map.of("count", service.unreadCount(user)));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> readAll(@AuthenticationPrincipal User user) {
        return ApiResponse.ok("All notifications marked as read", Map.of("updated", service.markAllRead(user)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationDto> read(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(service.markRead(user, id));
    }
}
