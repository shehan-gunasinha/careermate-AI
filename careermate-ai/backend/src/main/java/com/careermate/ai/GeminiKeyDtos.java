package com.careermate.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class GeminiKeyDtos {
    private GeminiKeyDtos() {
    }

    public record GeminiKeyRequest(
            @NotBlank(message = "Enter your Gemini API key")
            @Size(min = 20, max = 512, message = "Gemini API keys are between 20 and 512 characters")
            String apiKey) {
    }

    public record GeminiKeyStatus(
            boolean configured,
            String hint,
            boolean serverKeyConfigured,
            boolean aiEnabled,
            Instant updatedAt) {
    }
}
