package com.careermate.assistant;

import com.careermate.assistant.AssistantDtos.AssistantRequest;
import com.careermate.assistant.AssistantDtos.AssistantResponse;
import com.careermate.assistant.AssistantDtos.ConversationDto;
import com.careermate.assistant.AssistantDtos.MessageDto;
import com.careermate.assistant.AssistantDtos.StatusDto;
import com.careermate.common.response.ApiResponse;
import com.careermate.user.User;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/career-assistant")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService service;

    @PostMapping
    public ApiResponse<AssistantResponse> chat(@AuthenticationPrincipal User user, @Valid @RequestBody AssistantRequest req) {
        return ApiResponse.ok(service.chat(user, req));
    }

    @GetMapping("/status")
    public ApiResponse<StatusDto> status() {
        boolean ai = service.aiEnabled();
        return ApiResponse.ok(new StatusDto(ai, ai ? "AI" : "FALLBACK"));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationDto>> conversations(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.conversations(user));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageDto>> messages(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.ok(service.messages(user, id));
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        service.delete(user, id);
        return ApiResponse.ok("Conversation deleted", null);
    }
}
