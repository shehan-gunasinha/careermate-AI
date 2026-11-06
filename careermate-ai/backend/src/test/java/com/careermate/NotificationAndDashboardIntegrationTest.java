package com.careermate;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationAndDashboardIntegrationTest extends IntegrationTestBase {

    @Test
    void notificationsCanBeMarkedReadAndAreIsolated() throws Exception {
        String u1 = registerUser();
        String u2 = registerUser();
        var list = body(mvc.perform(auth(get("/api/v1/notifications"), u1)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].read").value(false)).andReturn());
        long id = list.at("/data/content/0/id").asLong();
        mvc.perform(auth(get("/api/v1/notifications/unread-count"), u1)).andExpect(jsonPath("$.data.count").value(1));
        mvc.perform(auth(patch("/api/v1/notifications/" + id + "/read"), u2)).andExpect(status().isNotFound());
        mvc.perform(auth(patch("/api/v1/notifications/" + id + "/read"), u1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.read").value(true));
        mvc.perform(auth(get("/api/v1/notifications/unread-count"), u1)).andExpect(jsonPath("$.data.count").value(0));
        mvc.perform(auth(post("/api/v1/applications"), u1).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("jobTitle", "A", "company", "B", "status", "APPLIED")))).andExpect(status().isCreated());
        mvc.perform(auth(patch("/api/v1/notifications/read-all"), u1)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/notifications").param("unreadOnly", "true"), u1)).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void dashboardNumbersComeFromTheDatabase() throws Exception {
        String admin = adminToken();
        long job = createJob(admin, jobBody("Dash " + unique(), "DashCo" + unique(), "Colombo", "FULL_TIME", "JUNIOR", "HYBRID", "IT", List.of("Java"), null, null));
        String user = registerUser();
        mvc.perform(auth(get("/api/v1/dashboard"), user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savedJobs").value(0)).andExpect(jsonPath("$.data.applications").value(0))
                .andExpect(jsonPath("$.data.suggestions").isNotEmpty());
        mvc.perform(auth(post("/api/v1/jobs/" + job + "/save"), user)).andExpect(status().isOk());
        for (String st : List.of("APPLIED", "INTERVIEW_SCHEDULED", "REJECTED", "REJECTED", "OFFER_RECEIVED")) {
            mvc.perform(auth(post("/api/v1/applications"), user).contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("jobTitle", "T-" + st, "company", "C", "status", st)))).andExpect(status().isCreated());
        }
        mvc.perform(auth(get("/api/v1/dashboard"), user))
                .andExpect(jsonPath("$.data.savedJobs").value(1))
                .andExpect(jsonPath("$.data.applications").value(5))
                .andExpect(jsonPath("$.data.interviews").value(1))
                .andExpect(jsonPath("$.data.rejected").value(2))
                .andExpect(jsonPath("$.data.offers").value(1))
                .andExpect(jsonPath("$.data.statusBreakdown.REJECTED").value(2))
                .andExpect(jsonPath("$.data.recentActivities").isNotEmpty())
                .andExpect(jsonPath("$.data.recentJobs").isArray())
                .andExpect(jsonPath("$.data.recommendedJobs").isArray())
                .andExpect(jsonPath("$.data.aiEnabled").value(false));
        mvc.perform(get("/api/v1/dashboard")).andExpect(status().isUnauthorized());
    }
}
