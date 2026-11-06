package com.careermate.auth;

import com.careermate.auth.AuthDtos.AuthResponse;
import com.careermate.auth.AuthDtos.ChangePasswordRequest;
import com.careermate.auth.AuthDtos.LoginRequest;
import com.careermate.auth.AuthDtos.RegisterRequest;
import com.careermate.auth.AuthDtos.UserDto;
import com.careermate.common.exception.BadRequestException;
import com.careermate.common.exception.ConflictException;
import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.exception.UnauthorizedException;
import com.careermate.notifications.NotificationService;
import com.careermate.notifications.NotificationType;
import com.careermate.profile.ProfileService;
import com.careermate.security.JwtService;
import com.careermate.security.TokenBlacklist;
import com.careermate.user.Role;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import io.jsonwebtoken.Claims;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Pattern STRENGTH = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final ProfileService profileService;
    private final NotificationService notificationService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        validateStrength(req.password());
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.JOB_SEEKER);
        user = userRepository.save(user);
        profileService.ensureProfile(user);
        notificationService.create(user, NotificationType.SYSTEM, "Welcome to CareerMate AI",
                "Complete your profile and upload your resume to get personalised job recommendations.", "/profile", "welcome");
        log.info("Registered user {}", user.getId());
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return buildResponse(user);
    }

    public void logout(String bearerHeader) {
        if (bearerHeader != null && bearerHeader.startsWith("Bearer ")) {
            try {
                Claims c = jwtService.parse(bearerHeader.substring(7).trim());
                blacklist.revoke(c.getId(), c.getExpiration().toInstant());
            } catch (Exception ignored) {
                // an invalid token needs no revocation
            }
        }
    }

    @Transactional
    public void changePassword(User principal, ChangePasswordRequest req) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        validateStrength(req.newPassword());
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), profileService.avatarUrl(u), u.getRole(), u.getCreatedAt());
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", jwtService.expiresInSeconds(), toDto(user));
    }

    private void validateStrength(String password) {
        if (!STRENGTH.matcher(password).matches()) {
            throw new BadRequestException("Password must be at least 8 characters and contain letters and numbers");
        }
    }
}
