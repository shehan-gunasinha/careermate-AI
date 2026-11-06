package com.careermate.profile;

import com.careermate.common.response.ApiResponse;
import com.careermate.profile.ProfileDtos.AvatarFile;
import com.careermate.profile.ProfileDtos.PreferencesDto;
import com.careermate.profile.ProfileDtos.ProfileDto;
import com.careermate.profile.ProfileDtos.ProfileUpdateRequest;
import com.careermate.user.User;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProfileDto> uploadAvatar(@AuthenticationPrincipal User user,
                                                @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Profile picture updated", profileService.uploadAvatar(user, file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<ProfileDto> removeAvatar(@AuthenticationPrincipal User user) {
        return ApiResponse.ok("Profile picture removed", profileService.removeAvatar(user));
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> avatar(@PathVariable Long userId) {
        AvatarFile file = profileService.getAvatar(userId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }
}
