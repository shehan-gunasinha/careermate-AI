package com.careermate.resume;

import java.util.List;
import java.util.Map;

public record AnalysisData(int overall, Map<String, Integer> sections, List<String> strengths, List<String> weaknesses,
                           List<String> missing, List<String> recommendations, List<String> languageIssues,
                           String summary) {
}
