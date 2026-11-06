package com.careermate;

import com.careermate.ai.GeminiClient;
import com.careermate.common.exception.AiUnavailableException;
import com.careermate.jobs.EmploymentType;
import com.careermate.jobs.ExperienceLevel;
import com.careermate.jobs.Job;
import com.careermate.jobs.JobRepository;
import com.careermate.jobs.JobSkill;
import com.careermate.jobs.WorkArrangement;
import com.careermate.matching.CandidateContext;
import com.careermate.matching.CandidateContextService;
import com.careermate.matching.JobMatchRepository;
import com.careermate.matching.MatchResult;
import com.careermate.matching.MatchingService;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchingServiceTest {
    @Mock JobRepository jobRepository;
    @Mock JobMatchRepository matchRepository;
    @Mock UserRepository userRepository;
    @Mock CandidateContextService contextService;
    @Mock GeminiClient gemini;
    MatchingService service;

    @BeforeEach
    void setUp() {
        service = new MatchingService(jobRepository, matchRepository, userRepository, contextService, gemini);
    }

    private Job job(String title, ExperienceLevel level, String industry, String qualifications, String... required) {
        Job j = new Job();
        j.setTitle(title);
        j.setCompanyName("Demo Co");
        j.setLocation("Colombo");
        j.setIndustry(industry);
        j.setEmploymentType(EmploymentType.FULL_TIME);
        j.setWorkArrangement(WorkArrangement.ON_SITE);
        j.setExperienceLevel(level);
        j.setDescription("Description for " + title);
        j.setRequiredQualifications(qualifications);
        for (String s : required) {
            j.getSkills().add(new JobSkill(j, s, true));
        }
        return j;
    }

    private CandidateContext candidate(Integer years, int eduRank, List<String> skills, String resume) {
        return new CandidateContext(1L, "Test", null, null, years, skills, List.of(), eduRank, List.of(), List.of(), List.of(), List.of(), resume);
    }

    @Test
    void strongCandidateScoresHighWithNoMissingSkills() {
        Job j = job("Junior Software Engineer", ExperienceLevel.JUNIOR, "IT", "Bachelor's degree in Computer Science", "Java", "Spring Boot", "SQL");
        MatchResult r = service.basic(candidate(2, 3, List.of("Java", "spring boot", "SQL"), null), j);
        assertTrue(r.score() >= 85, "score " + r.score());
        assertTrue(r.missingSkills().isEmpty());
        assertEquals(3, r.matchingSkills().size());
        assertFalse(r.aiPowered());
    }

    @Test
    void missingSkillsAreListedWithRecommendations() {
        Job j = job("Junior Software Engineer", ExperienceLevel.JUNIOR, "IT", null, "Java", "Kubernetes", "AWS");
        MatchResult r = service.basic(candidate(1, 3, List.of("Java"), null), j);
        assertEquals(List.of("Kubernetes", "AWS"), r.missingSkills());
        assertTrue(r.recommendations().stream().anyMatch(s -> s.contains("Kubernetes")));
        assertTrue(r.score() < 80);
    }

    @Test
    void synonymsAndResumeTextCountAsMatches() {
        Job j = job("DevOps Engineer", ExperienceLevel.MID, "IT", null, "Kubernetes", "PostgreSQL", "JavaScript");
        MatchResult r = service.basic(candidate(4, 3, List.of("k8s", "postgres"), "Experienced with JavaScript frameworks"), j);
        assertTrue(r.missingSkills().isEmpty(), "missing " + r.missingSkills());
    }

    @Test
    void experienceGapLowersScore() {
        Job j = job("Senior Accountant", ExperienceLevel.SENIOR, "Finance", null, "Accounting");
        MatchResult junior = service.basic(candidate(1, 3, List.of("Accounting"), null), j);
        MatchResult senior = service.basic(candidate(8, 3, List.of("Accounting"), null), j);
        assertTrue(junior.experienceScore() < senior.experienceScore());
        assertTrue(junior.score() < senior.score());
        assertEquals(100, senior.experienceScore());
    }

    @Test
    void educationRequirementIsCompared() {
        Job j = job("Analyst", ExperienceLevel.ENTRY, "Business", "Master's degree in Economics", "Excel");
        assertEquals(100, service.basic(candidate(0, 4, List.of("Excel"), null), j).educationScore());
        assertTrue(service.basic(candidate(0, 3, List.of("Excel"), null), j).educationScore() < 100);
    }

    @Test
    void worksWithoutListedSkillsAndForNonTechJobs() {
        Job j = job("Front Desk Associate", ExperienceLevel.ENTRY, "Hospitality", null);
        MatchResult r = service.basic(candidate(null, 0, List.of(), null), j);
        assertTrue(r.score() >= 0 && r.score() <= 100);
        assertFalse(r.recommendations().isEmpty());
    }

    @Test
    void aiResultIsBlendedWhenGeminiResponds() throws Exception {
        Job j = job("Developer", ExperienceLevel.JUNIOR, "IT", null, "Java", "Kubernetes");
        j.setId(5L);
        CandidateContext ctx = candidate(1, 3, List.of("Java"), null);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(j));
        when(contextService.build(any(User.class))).thenReturn(ctx);
        when(matchRepository.findByUserIdAndJobId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.generateJson(anyString(), anyString())).thenReturn(new ObjectMapper().readTree(
                "{\"score\":100,\"matchingSkills\":[\"Java\"],\"missingSkills\":[\"Kubernetes\"],\"strengths\":[\"Good\"],\"improvements\":[\"Learn K8s\"],\"recommendations\":[\"Do a course\"]}"));
        User u = new User();
        u.setId(1L);
        MatchResult base = service.basic(ctx, j);
        MatchResult r = service.match(u, 5L);
        assertTrue(r.aiPowered());
        assertEquals(Math.round(100 * 0.6 + base.score() * 0.4), r.score());
        assertEquals(List.of("Do a course"), r.recommendations());
    }

    @Test
    void fallsBackToAlgorithmWhenAiFails() {
        Job j = job("Developer", ExperienceLevel.JUNIOR, "IT", null, "Java");
        j.setId(6L);
        when(jobRepository.findById(6L)).thenReturn(Optional.of(j));
        when(contextService.build(any(User.class))).thenReturn(candidate(1, 3, List.of("Java"), null));
        when(matchRepository.findByUserIdAndJobId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.generateJson(anyString(), anyString())).thenThrow(new AiUnavailableException("timeout"));
        User u = new User();
        u.setId(1L);
        MatchResult r = service.match(u, 6L);
        assertFalse(r.aiPowered());
        assertTrue(r.score() > 0);
    }
}
