package com.careermate.profile;

import com.careermate.common.exception.ResourceNotFoundException;
import com.careermate.common.util.Json;
import com.careermate.profile.ProfileDtos.EducationDto;
import com.careermate.profile.ProfileDtos.ExperienceDto;
import com.careermate.profile.ProfileDtos.PreferencesDto;
import com.careermate.profile.ProfileDtos.ProfileDto;
import com.careermate.profile.ProfileDtos.ProfileUpdateRequest;
import com.careermate.resume.ResumeRepository;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {
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

        return new ProfileDto(user.getFullName(), user.getEmail(),
                p == null ? null : p.getPhone(), p == null ? null : p.getLocation(),
                p == null ? null : p.getHeadline(), p == null ? null : p.getCareerObjective(),
                p == null ? null : p.getEmploymentStatus(), p == null ? null : p.getYearsOfExperience(),
                skills, edu, exp, prefDto, pct);
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
