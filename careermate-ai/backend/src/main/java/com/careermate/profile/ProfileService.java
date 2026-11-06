package com.careermate.profile;

import com.careermate.common.exception.BadRequestException;
import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.util.Json;
import com.careermate.profile.ProfileDtos.AvatarFile;
import com.careermate.profile.ProfileDtos.EducationDto;
import com.careermate.profile.ProfileDtos.ExperienceDto;
import com.careermate.profile.ProfileDtos.PreferencesDto;
import com.careermate.profile.ProfileDtos.ProfileDto;
import com.careermate.profile.ProfileDtos.ProfileUpdateRequest;
import com.careermate.resume.ResumeRepository;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UserSkillRepository skillRepository;
    private final UserEducationRepository educationRepository;
    private final UserExperienceRepository experienceRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public UserProfile ensureProfile(User user) {
        User managed = userRepository.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return profileRepository.findByUserId(managed.getId()).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUser(managed);
            return profileRepository.save(p);
        });
    }

    private UserPreference ensurePreference(User managed) {
        return preferenceRepository.findByUserId(managed.getId()).orElseGet(() -> {
            UserPreference p = new UserPreference();
            p.setUser(managed);
            return preferenceRepository.save(p);
        });
    }

    @Transactional
    public ProfileDto getProfile(User user) {
        ensureProfile(user);
        return toDto(userRepository.findById(user.getId()).orElseThrow());
    }

    @Transactional
    public ProfileDto updateProfile(User user, ProfileUpdateRequest req) {
        User managed = userRepository.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        managed.setFullName(req.fullName().trim());
        UserProfile p = ensureProfile(managed);
        p.setPhone(blankToNull(req.phone()));
        p.setLocation(blankToNull(req.location()));
        p.setHeadline(blankToNull(req.headline()));
        p.setCareerObjective(blankToNull(req.careerObjective()));
        p.setEmploymentStatus(req.employmentStatus());
        p.setYearsOfExperience(req.yearsOfExperience());
        profileRepository.save(p);

        if (req.skills() != null) {
            skillRepository.deleteAllByUserId(managed.getId());
            Map<String, String> unique = new LinkedHashMap<>();
            for (String s : req.skills()) {
                if (s != null && !s.isBlank()) {
                    unique.putIfAbsent(s.trim().toLowerCase(), s.trim());
                }
            }
            List<UserSkill> skills = new ArrayList<>();
            for (String name : unique.values()) {
                UserSkill us = new UserSkill();
                us.setUser(managed);
                us.setName(name);
                skills.add(us);
            }
            skillRepository.saveAll(skills);
        }
        if (req.education() != null) {
            educationRepository.deleteAllByUserId(managed.getId());
            List<UserEducation> list = new ArrayList<>();
            for (EducationDto d : req.education()) {
                UserEducation e = new UserEducation();
                e.setUser(managed);
                e.setInstitution(d.institution().trim());
                e.setDegree(blankToNull(d.degree()));
                e.setFieldOfStudy(blankToNull(d.fieldOfStudy()));
                e.setStartYear(d.startYear());
                e.setEndYear(d.endYear());
                e.setDescription(blankToNull(d.description()));
                list.add(e);
            }
            educationRepository.saveAll(list);
        }
        if (req.experience() != null) {
            experienceRepository.deleteAllByUserId(managed.getId());
            List<UserExperience> list = new ArrayList<>();
            for (ExperienceDto d : req.experience()) {
                UserExperience e = new UserExperience();
                e.setUser(managed);
                e.setCompany(d.company().trim());
                e.setTitle(d.title().trim());
                e.setStartDate(d.startDate());
                e.setEndDate(d.current() ? null : d.endDate());
                e.setCurrent(d.current());
                e.setDescription(blankToNull(d.description()));
                list.add(e);
            }
            experienceRepository.saveAll(list);
        }
        userRepository.save(managed);
        return toDto(managed);
    }

    @Transactional
    public ProfileDto uploadAvatar(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BadRequestException("Image is too large. Maximum allowed size is 2 MB.");
        }
        String original = sanitizeName(file.getOriginalFilename());
        String ext = extension(original);
        if (!AVATAR_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("Only PNG, JPG and WEBP images are supported");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("The image could not be read");
        }
        verifyImageBytes(bytes, ext);

        User managed = userRepository.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserProfile p = ensureProfile(managed);
        p.setAvatarData(bytes);
        p.setAvatarContentType(imageContentType(ext));
        p.setAvatarUpdatedAt(Instant.now());
        profileRepository.saveAndFlush(p);
        return toDto(managed);
    }

    @Transactional
    public ProfileDto removeAvatar(User user) {
        User managed = userRepository.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserProfile p = ensureProfile(managed);
        p.setAvatarData(null);
        p.setAvatarContentType(null);
        p.setAvatarUpdatedAt(null);
        profileRepository.saveAndFlush(p);
        return toDto(managed);
    }

    @Transactional(readOnly = true)
    public AvatarFile getAvatar(Long userId) {
        UserProfile p = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Avatar not found"));
        if (p.getAvatarData() == null || p.getAvatarData().length == 0) {
            throw new ResourceNotFoundException("Avatar not found");
        }
        String contentType = p.getAvatarContentType() == null ? "image/jpeg" : p.getAvatarContentType();
        return new AvatarFile(p.getAvatarData(), contentType);
    }

    @Transactional(readOnly = true)
    public String avatarUrl(User user) {
        return profileRepository.findAvatarUpdatedAtByUserId(user.getId())
                .map(updatedAt -> avatarUrl(user.getId(), updatedAt))
                .orElse(null);
    }

    @Transactional
    public PreferencesDto updatePreferences(User user, PreferencesDto dto) {
        User managed = userRepository.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserPreference pref = ensurePreference(managed);
        pref.setPreferredIndustries(Json.write(clean(dto.preferredIndustries())));
        pref.setPreferredRoles(Json.write(clean(dto.preferredRoles())));
        pref.setPreferredLocations(Json.write(clean(dto.preferredLocations())));
        preferenceRepository.save(pref);
        return toPreferences(pref);
    }

    @Transactional(readOnly = true)
    public int completionPercentage(User user) {
        return toDto(user).completionPercentage();
    }

    private ProfileDto toDto(User user) {
        UserProfile p = profileRepository.findByUserId(user.getId()).orElse(null);
        List<String> skills = skillRepository.findByUserIdOrderByIdAsc(user.getId()).stream().map(UserSkill::getName).toList();
        List<EducationDto> edu = educationRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .map(e -> new EducationDto(e.getId(), e.getInstitution(), e.getDegree(), e.getFieldOfStudy(),
                        e.getStartYear(), e.getEndYear(), e.getDescription())).toList();
        List<ExperienceDto> exp = experienceRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
                .map(e -> new ExperienceDto(e.getId(), e.getCompany(), e.getTitle(), e.getStartDate(), e.getEndDate(),
                        e.isCurrent(), e.getDescription())).toList();
        UserPreference pref = preferenceRepository.findByUserId(user.getId()).orElse(null);
        PreferencesDto prefDto = pref == null ? new PreferencesDto(List.of(), List.of(), List.of()) : toPreferences(pref);

        int filled = 0;
        int total = 11;
        if (p != null) {
            if (notBlank(p.getPhone())) filled++;
            if (notBlank(p.getLocation())) filled++;
            if (notBlank(p.getHeadline())) filled++;
            if (notBlank(p.getCareerObjective())) filled++;
            if (p.getEmploymentStatus() != null) filled++;
            if (p.getYearsOfExperience() != null) filled++;
        }
        if (skills.size() >= 3) filled++;
        if (!edu.isEmpty()) filled++;
        if (!exp.isEmpty()) filled++;
        if (!prefDto.preferredIndustries().isEmpty() || !prefDto.preferredRoles().isEmpty()) filled++;
        if (resumeRepository.existsByUserId(user.getId())) filled++;
        int pct = Math.round(filled * 100f / total);

        String avatarUrl = p == null || p.getAvatarUpdatedAt() == null ? null : avatarUrl(user.getId(), p.getAvatarUpdatedAt());
        return new ProfileDto(user.getFullName(), user.getEmail(), avatarUrl,
                p == null ? null : p.getPhone(), p == null ? null : p.getLocation(),
                p == null ? null : p.getHeadline(), p == null ? null : p.getCareerObjective(),
                p == null ? null : p.getEmploymentStatus(), p == null ? null : p.getYearsOfExperience(),
                skills, edu, exp, prefDto, pct);
    }

    private static String avatarUrl(Long userId, Instant updatedAt) {
        return "/api/v1/profile/avatar/" + userId + "?v=" + updatedAt.toEpochMilli();
    }

    private static String imageContentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "avatar";
        }
        String n = name.replace("\\", "/");
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_");
        return n.length() > 150 ? n.substring(n.length() - 150) : n;
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static void verifyImageBytes(byte[] b, String ext) {
        boolean ok;
        if ("png".equals(ext)) {
            ok = b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        } else if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            ok = b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        } else {
            ok = b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        }
        if (!ok) {
            throw new BadRequestException("The image content does not match its extension");
        }
    }

    private PreferencesDto toPreferences(UserPreference p) {
        return new PreferencesDto(Json.readStringList(p.getPreferredIndustries()),
                Json.readStringList(p.getPreferredRoles()), Json.readStringList(p.getPreferredLocations()));
    }

    private static List<String> clean(List<String> in) {
        if (in == null) {
            return List.of();
        }
        return in.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).distinct().toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
