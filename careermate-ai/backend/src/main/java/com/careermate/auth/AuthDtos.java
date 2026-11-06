package com.careermate.auth;

import com.careermate.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(@NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
                                  @NotBlank(message = "Email is required") @Email(message = "Email is invalid") @Size(max = 255) String email,
                                  @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password,
                                  @NotBlank(message = "Please confirm your password") String confirmPassword) {
    }

    public record LoginRequest(@NotBlank(message = "Email is required") @Email(message = "Email is invalid") String email,
                               @NotBlank(message = "Password is required") String password) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String newPassword,
                                        @NotBlank String confirmPassword) {
    }

    public record UserDto(Long id, String fullName, String email, String avatarUrl, Role role, Instant createdAt) {
    }

    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserDto user) {
    }
}
