package com.careermate.matching;

import com.careermate.common.util.Json;
import com.careermate.profile.UserEducation;
import com.careermate.profile.UserEducationRepository;
import com.careermate.profile.UserExperience;
import com.careermate.profile.UserExperienceRepository;
import com.careermate.profile.UserPreference;
import com.careermate.profile.UserPreferenceRepository;
import com.careermate.profile.UserProfile;
import com.careermate.profile.UserProfileRepository;
import com.careermate.profile.UserSkill;
import com.careermate.profile.UserSkillRepository;
import com.careermate.resume.Resume;
import com.careermate.resume.ResumeRepository;
import com.careermate.user.User;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CandidateContextService {
    private final UserProfileRepository profileRepository;
    private final UserSkillRepository skillRepository;
    private final UserEducationRepository educationRepository;
    private final UserExperienceRepository experienceRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final ResumeRepository resumeRepository;

    @Transactional(readOnly = true)
    public CandidateContext build(User user) {
        return build(user, true);
    }

    @Transactional(readOnly = true)
    public CandidateContext build(User user, boolean includeResume) {
        UserProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        List<String> skills = skillRepository.findByUserIdOrderByIdAsc(user.getId()).stream().map(UserSkill::getName).toList();
        List<UserEducation> eduEntities = educationRepository.findByUserIdOrderByIdAsc(user.getId());
        List<String> edu = new ArrayList<>();
        for (UserEducation e : eduEntities) {
            edu.add(join(e.getDegree(), e.getFieldOfStudy(), e.getInstitution()));
        }
        List<UserExperience> expEntities = experienceRepository.findByUserIdOrderByIdAsc(user.getId());
        List<String> exp = new ArrayList<>();
        long months = 0;
        for (UserExperience e : expEntities) {
            exp.add(join(e.getTitle(), e.getCompany()) + (e.getDescription() == null ? "" : " - " + e.getDescription()));
            if (e.getStartDate() != null) {
                LocalDate end = e.isCurrent() || e.getEndDate() == null ? LocalDate.now() : e.getEndDate();
                months += Math.max(0, ChronoUnit.MONTHS.between(e.getStartDate(), end));
            }
        }
        Integer years = profile != null ? profile.getYearsOfExperience() : null;
        if (years == null && !expEntities.isEmpty() && months > 0) {
            years = (int) Math.round(months / 12.0);
        }
        UserPreference pref = preferenceRepository.findByUserId(user.getId()).orElse(null);
        String resumeText = null;
        if (includeResume) {
            resumeText = resumeRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                    .map(Resume::getExtractedText).orElse(null);
            if (resumeText != null && resumeText.length() > 8000) {
                resumeText = resumeText.substring(0, 8000);
            }
        }
        String eduBlob = String.join(" ", edu) + " " + (resumeText == null ? "" : resumeText);
        return new CandidateContext(user.getId(), user.getFullName(),
                profile == null ? null : profile.getHeadline(),
                profile == null ? null : profile.getCareerObjective(),
                years, skills, edu, detectEducationRank(eduBlob), exp,
                pref == null ? List.of() : Json.readStringList(pref.getPreferredIndustries()),
                pref == null ? List.of() : Json.readStringList(pref.getPreferredRoles()),
                pref == null ? List.of() : Json.readStringList(pref.getPreferredLocations()),
                resumeText);
    }

    /** 0 = unknown, 1 = school/certificate, 2 = diploma, 3 = bachelor, 4 = master, 5 = doctorate. */
    public static int detectEducationRank(String text) {
        if (text == null) {
            return 0;
        }
        String t = text.toLowerCase(Locale.ROOT);
        if (t.matches("(?s).*\\b(ph\\.?d|doctorate|doctoral)\\b.*")) return 5;
        if (t.matches("(?s).*\\b(master|msc|m\\.sc|mba|m\\.eng|meng|postgraduate)\\b.*")) return 4;
        if (t.matches("(?s).*\\b(bachelor|bsc|b\\.sc|b\\.eng|beng|b\\.a|undergraduate|bba|degree)\\b.*")) return 3;
        if (t.matches("(?s).*\\b(diploma|hnd|associate degree|higher national)\\b.*")) return 2;
        if (t.matches("(?s).*\\b(certificate|high school|secondary school|a/l|advanced level|o/l|ged)\\b.*")) return 1;
        return 0;
    }

    private static String join(String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                out.add(p.trim());
            }
        }
        return String.join(", ", out);
    }
}
