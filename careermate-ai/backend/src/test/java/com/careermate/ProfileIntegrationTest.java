package com.careermate;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileIntegrationTest extends IntegrationTestBase {

    @Test
    void newUserHasEmptyProfile() throws Exception {
        String token = registerUser();
        mvc.perform(auth(get("/api/v1/profile"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.completionPercentage").value(0))
                .andExpect(jsonPath("$.data.skills.length()").value(0));
    }

    @Test
    void profileUpdateIsPersistedAndRaisesCompletion() throws Exception {
        String token = registerUser();
        Map<String, Object> req = Map.of(
                "fullName", "Updated Name", "phone", "+94 70 111 2222", "location", "Colombo", "headline", "Junior Developer",
                "careerObjective", "Grow as an engineer.", "employmentStatus", "STUDENT", "yearsOfExperience", 1,
                "skills", List.of("Java", "SQL", "Git", "java"),
                "education", List.of(Map.of("institution", "Sample Uni", "degree", "BSc", "fieldOfStudy", "CS", "startYear", 2020, "endYear", 2024)),
                "experience", List.of(Map.of("company", "Sample Ltd", "title", "Intern", "startDate", "2023-01-01", "endDate", "2023-06-30", "current", false)));
        mvc.perform(auth(put("/api/v1/profile"), token).contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.data.skills.length()").value(3));
        mvc.perform(auth(get("/api/v1/profile"), token))
                .andExpect(jsonPath("$.data.headline").value("Junior Developer"))
                .andExpect(jsonPath("$.data.education[0].institution").value("Sample Uni"))
                .andExpect(jsonPath("$.data.experience[0].title").value("Intern"));
        mvc.perform(auth(get("/api/v1/auth/me"), token)).andExpect(jsonPath("$.data.fullName").value("Updated Name"));
        String pct = body(mvc.perform(auth(get("/api/v1/profile"), token)).andReturn()).at("/data/completionPercentage").asText();
        assertTrue(Integer.parseInt(pct) >= 70, "completion should be high, was " + pct);
    }

    @Test
    void replacingSkillsTwiceDoesNotViolateUniqueConstraint() throws Exception {
        String token = registerUser();
        for (int i = 0; i < 2; i++) {
            mvc.perform(auth(put("/api/v1/profile"), token).contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("fullName", "T U", "skills", List.of("Java", "SQL")))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void preferencesAreStored() throws Exception {
        String token = registerUser();
        mvc.perform(auth(put("/api/v1/profile/preferences"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("preferredIndustries", List.of("Finance"), "preferredRoles", List.of("Accountant"),
                                "preferredLocations", List.of("Colombo, Sri Lanka")))))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/profile"), token))
                .andExpect(jsonPath("$.data.preferences.preferredLocations[0]").value("Colombo, Sri Lanka"))
                .andExpect(jsonPath("$.data.preferences.preferredIndustries[0]").value("Finance"));
    }

    @Test
    void invalidProfileIsRejected() throws Exception {
        String token = registerUser();
        mvc.perform(auth(put("/api/v1/profile"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", " ", "yearsOfExperience", 120))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.yearsOfExperience").exists());
    }
}
