package com.careermate;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationIntegrationTest extends IntegrationTestBase {

    private long create(String token, Map<String, Object> body) throws Exception {
        return body(mvc.perform(auth(post("/api/v1/applications"), token).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
    }

    @Test
    void fullLifecycleWithHistory() throws Exception {
        String token = registerUser();
        long id = create(token, Map.of("jobTitle", "Analyst", "company", "Acme", "status", "APPLIED", "notes", "first note"));
        mvc.perform(auth(get("/api/v1/applications/" + id), token))
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.applicationDate").isNotEmpty());
        mvc.perform(auth(put("/api/v1/applications/" + id), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("jobTitle", "Senior Analyst", "company", "Acme", "status", "APPLIED", "contactPerson", "Pat",
                                "interviewDate", "2030-01-15T10:30:00", "notes", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobTitle").value("Senior Analyst"))
                .andExpect(jsonPath("$.data.contactPerson").value("Pat"))
                .andExpect(jsonPath("$.data.interviewDate").value("2030-01-15T10:30:00"));
        mvc.perform(auth(patch("/api/v1/applications/" + id + "/status"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERVIEW_SCHEDULED", "note", "Phone screen booked"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"));
        mvc.perform(auth(get("/api/v1/applications/" + id + "/history"), token))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].toStatus").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data[0].fromStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[0].note").value("Phone screen booked"));
        // status change produced a notification
        mvc.perform(auth(get("/api/v1/notifications"), token)).andExpect(jsonPath("$.data.totalElements").value(2)); // welcome + status
        mvc.perform(auth(delete("/api/v1/applications/" + id), token)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/applications/" + id), token)).andExpect(status().isNotFound());
    }

    @Test
    void validationAndFilters() throws Exception {
        String token = registerUser();
        mvc.perform(auth(post("/api/v1/applications"), token).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("company", "NoTitle"))))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/v1/applications"), token).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("jobTitle", "X", "company", "Y", "status", "BOGUS"))))
                .andExpect(status().isBadRequest());
        create(token, Map.of("jobTitle", "Alpha Role", "company", "Zeta", "status", "SAVED"));
        create(token, Map.of("jobTitle", "Beta Role", "company", "Zeta", "status", "REJECTED"));
        create(token, Map.of("jobTitle", "Gamma", "company", "Omega", "status", "APPLIED"));
        mvc.perform(auth(get("/api/v1/applications"), token)).andExpect(jsonPath("$.data.totalElements").value(3));
        mvc.perform(auth(get("/api/v1/applications").param("status", "REJECTED"), token)).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(auth(get("/api/v1/applications").param("q", "zeta"), token)).andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(auth(get("/api/v1/applications").param("sortBy", "company").param("asc", "true"), token))
                .andExpect(jsonPath("$.data.content[0].company").value("Omega"));
    }

    @Test
    void applicationCanBeLinkedToJobAndInheritsDetails() throws Exception {
        String admin = adminToken();
        long jobId = createJob(admin, jobBody("Linked " + unique(), "LinkCo" + unique(), "Colombo", "FULL_TIME", "JUNIOR", "HYBRID", "IT", List.of("Java"), null, null));
        String token = registerUser();
        long id = create(token, Map.of("jobId", jobId, "status", "SAVED"));
        mvc.perform(auth(get("/api/v1/applications/" + id), token))
                .andExpect(jsonPath("$.data.jobId").value(jobId))
                .andExpect(jsonPath("$.data.jobTitle").value(org.hamcrest.Matchers.startsWith("Linked ")));
        mvc.perform(auth(post("/api/v1/applications"), token).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("jobId", 9999999))))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotAccessEachOthersApplications() throws Exception {
        String owner = registerUser();
        String other = registerUser();
        long id = create(owner, Map.of("jobTitle", "Secret Role", "company", "Secret Co", "status", "APPLIED"));
        mvc.perform(auth(get("/api/v1/applications/" + id), other)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/api/v1/applications/" + id), other).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("jobTitle", "Hack", "company", "Hack"))))
                .andExpect(status().isNotFound());
        mvc.perform(auth(patch("/api/v1/applications/" + id + "/status"), other).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "REJECTED"))))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/v1/applications/" + id), other)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/applications/" + id + "/history"), other)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/applications"), other)).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(auth(get("/api/v1/applications/" + id), owner)).andExpect(status().isOk());
    }
}
