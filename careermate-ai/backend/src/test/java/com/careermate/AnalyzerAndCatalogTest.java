package com.careermate;

import com.careermate.common.util.SkillCatalog;
import com.careermate.resume.AnalysisData;
import com.careermate.resume.RuleBasedResumeAnalyzer;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerAndCatalogTest {
    private final RuleBasedResumeAnalyzer analyzer = new RuleBasedResumeAnalyzer();

    private static final String GOOD = String.join("\n", IntegrationTestBase.SAMPLE_RESUME);
    private static final String WEAK = "i worked at a shop. I have experiance and i am a hard hard worker";

    @Test
    void goodResumeScoresHigherThanWeakResume() {
        AnalysisData good = analyzer.analyze(GOOD, "docx");
        AnalysisData weak = analyzer.analyze(WEAK, "docx");
        assertTrue(good.overall() > weak.overall() + 20, good.overall() + " vs " + weak.overall());
        assertEquals(9, good.sections().size());
        assertTrue(good.sections().values().stream().allMatch(v -> v >= 0 && v <= 100));
    }

    @Test
    void weakResumeReportsMissingInformationAndLanguageIssues() {
        AnalysisData weak = analyzer.analyze(WEAK, "pdf");
        assertTrue(weak.missing().stream().anyMatch(s -> s.toLowerCase().contains("email")));
        assertTrue(weak.missing().stream().anyMatch(s -> s.toLowerCase().contains("summary")));
        assertTrue(weak.languageIssues().stream().anyMatch(s -> s.contains("experiance")));
        assertTrue(weak.languageIssues().stream().anyMatch(s -> s.contains("hard hard")));
        assertFalse(weak.recommendations().isEmpty());
        assertFalse(weak.weaknesses().isEmpty());
    }

    @Test
    void emptyTextDoesNotCrash() {
        AnalysisData d = analyzer.analyze("", "pdf");
        assertTrue(d.overall() >= 0 && d.overall() < 40);
    }

    @Test
    void skillCatalogNormalisesAndFindsSkills() {
        assertEquals("kubernetes", SkillCatalog.normalize("K8s"));
        assertEquals("postgresql", SkillCatalog.normalize("  Postgres "));
        List<String> found = SkillCatalog.findIn("We use Java, Spring Boot and Excel; no javascript here? Actually JavaScript yes.");
        assertTrue(found.containsAll(List.of("Java", "Spring Boot", "Excel", "JavaScript")));
        assertFalse(SkillCatalog.containsTerm("javascript developer", "java"), "java must not match inside javascript");
        assertTrue(SkillCatalog.containsTerm("c# and .net", "c#"));
    }
}
