package com.careermate.matching;

import com.careermate.common.util.SkillCatalog;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Snapshot of everything we know about a candidate; shared by matching, resume and assistant features. */
public record CandidateContext(Long userId, String fullName, String headline, String objective, Integer yearsExperience,
                               List<String> skills, List<String> education, int educationRank,
                               List<String> experience, List<String> industries, List<String> roles,
                               List<String> locations, String resumeText) {

    public Set<String> normalizedSkills() {
        Set<String> out = new LinkedHashSet<>();
        for (String s : skills) {
            out.add(SkillCatalog.normalize(s));
        }
        return out;
    }

    public boolean hasData() {
        return !skills.isEmpty() || !experience.isEmpty() || !education.isEmpty()
                || (resumeText != null && !resumeText.isBlank());
    }

    /** Compact text description used in AI prompts (never contains credentials). */
    public String toPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(fullName).append('\n');
        if (headline != null) sb.append("Headline: ").append(headline).append('\n');
        if (objective != null) sb.append("Career objective: ").append(objective).append('\n');
        if (yearsExperience != null) sb.append("Years of experience: ").append(yearsExperience).append('\n');
        if (!skills.isEmpty()) sb.append("Skills: ").append(String.join(", ", skills)).append('\n');
        if (!education.isEmpty()) sb.append("Education: ").append(String.join("; ", education)).append('\n');
        if (!experience.isEmpty()) sb.append("Experience: ").append(String.join("; ", experience)).append('\n');
        if (!industries.isEmpty()) sb.append("Preferred industries: ").append(String.join(", ", industries)).append('\n');
        if (!roles.isEmpty()) sb.append("Preferred roles: ").append(String.join(", ", roles)).append('\n');
        if (!locations.isEmpty()) sb.append("Preferred locations: ").append(String.join(", ", locations)).append('\n');
        if (resumeText != null && !resumeText.isBlank()) {
            sb.append("Resume text (excerpt):\n").append(resumeText, 0, Math.min(resumeText.length(), 3500)).append('\n');
        }
        return sb.toString();
    }
}
