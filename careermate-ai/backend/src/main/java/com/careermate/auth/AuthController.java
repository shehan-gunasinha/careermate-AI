package com.careermate.auth;

import com.careermate.auth.AuthDtos.AuthResponse;
import com.careermate.auth.AuthDtos.ChangePasswordRequest;
import com.careermate.auth.AuthDtos.ForgotPasswordRequest;
import com.careermate.auth.AuthDtos.LoginRequest;
import com.careermate.auth.AuthDtos.RegisterRequest;
import com.careermate.auth.AuthDtos.ResetPasswordRequest;
import com.careermate.auth.AuthDtos.UserDto;
import com.careermate.common.response.ApiResponse;
import com.careermate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("Registration successful", authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok("Login successful", authService.login(req));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.requestPasswordReset(req);
        return ApiResponse.ok("If an account exists for this email, a reset code has been sent.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ApiResponse.ok("Your password has been reset. You can now log in.", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(authService.toDto(user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
        return ApiResponse.ok("Logged out", null);
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(user, req);
        return ApiResponse.ok("Password changed", null);
    }
}
