package com.careermate.matching;

import java.util.List;

public record MatchResult(Long jobId, int score, List<String> matchingSkills, List<String> missingSkills,
                          int skillScore, int experienceScore, String experienceNote,
                          int educationScore, String educationNote, int preferenceScore,
                          List<String> strengths, List<String> improvements, List<String> recommendations,
                          boolean aiPowered) {
}
