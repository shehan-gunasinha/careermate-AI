package com.careermate.notifications;

import java.time.Instant;

public record NotificationDto(Long id, NotificationType type, String title, String message, String link, boolean read,
                              Instant createdAt) {
}
