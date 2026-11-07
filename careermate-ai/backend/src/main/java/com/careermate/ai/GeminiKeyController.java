package com.careermate.ai;

import com.careermate.ai.GeminiKeyDtos.GeminiKeyRequest;
import com.careermate.ai.GeminiKeyDtos.GeminiKeyStatus;
import com.careermate.common.response.ApiResponse;
import com.careermate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/gemini-key")
@RequiredArgsConstructor
public class GeminiKeyController {
    private final GeminiKeyService service;

    @GetMapping
    public ApiResponse<GeminiKeyStatus> status(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(service.status(user));
    }

    @PutMapping
    public ApiResponse<GeminiKeyStatus> save(@AuthenticationPrincipal User user, @Valid @RequestBody GeminiKeyRequest req) {
        return ApiResponse.ok("Gemini API key saved", service.save(user, req.apiKey()));
    }

    @DeleteMapping
    public ApiResponse<GeminiKeyStatus> delete(@AuthenticationPrincipal User user) {
        return ApiResponse.ok("Gemini API key removed", service.delete(user));
    }
}
