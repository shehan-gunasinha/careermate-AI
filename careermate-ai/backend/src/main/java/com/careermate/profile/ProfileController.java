package com.careermate.profile;

import com.careermate.common.response.ApiResponse;
import com.careermate.profile.ProfileDtos.PreferencesDto;
import com.careermate.profile.ProfileDtos.ProfileDto;
import com.careermate.profile.ProfileDtos.ProfileUpdateRequest;
import com.careermate.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileDto> get(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(profileService.getProfile(user));
    }

    @PutMapping
    public ApiResponse<ProfileDto> update(@AuthenticationPrincipal User user, @Valid @RequestBody ProfileUpdateRequest req) {
        return ApiResponse.ok("Profile updated", profileService.updateProfile(user, req));
    }

    @PutMapping("/preferences")
    public ApiResponse<PreferencesDto> preferences(@AuthenticationPrincipal User user, @Valid @RequestBody PreferencesDto req) {
        return ApiResponse.ok("Preferences updated", profileService.updatePreferences(user, req));
    }
}
