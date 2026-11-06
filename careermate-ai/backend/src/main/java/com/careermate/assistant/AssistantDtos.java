package com.careermate.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AssistantDtos {
    private AssistantDtos() {
    }

    public record AssistantRequest(@NotBlank(message = "Message is required") @Size(max = 4000, message = "Message is too long") String message,
                                   Long conversationId,
                                   Boolean useProfile) {
    }

    public record AssistantResponse(Long conversationId, String title, String reply, boolean aiPowered, Instant createdAt) {
    }

    public record ConversationDto(Long id, String title, Instant updatedAt) {
    }

    public record MessageDto(Long id, String role, String content, boolean aiPowered, Instant createdAt) {
    }

    public record StatusDto(boolean aiEnabled, String mode) {
    }
}
