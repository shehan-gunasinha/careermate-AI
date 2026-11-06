package com.careermate;

import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.careermate.jobs.EmploymentType;
import com.careermate.jobs.ExperienceLevel;
import com.careermate.jobs.JobService;
import com.careermate.jobs.JobService.UpsertOutcome;
import com.careermate.jobs.WorkArrangement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobIntegrationTest extends IntegrationTestBase {
    @Autowired JobService jobService;

    @Test
    void adminCanCreateJobsAndSeekersCannot() throws Exception {
        String tag = unique();
        Map<String, Object> job = jobBody("Tester " + tag, "Acme" + tag, "Colombo", "FULL_TIME", "JUNIOR", "HYBRID", "Information Technology", List.of("Java"), 1000, 2000);
        mvc.perform(auth(post("/api/v1/admin/jobs"), registerUser()).contentType(MediaType.APPLICATION_JSON).content(json(job)))
                .andExpect(status().isForbidden());
        String admin = adminToken();
        mvc.perform(auth(post("/api/v1/admin/jobs"), admin).contentType(MediaType.APPLICATION_JSON).content(json(job)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.source").value("MANUAL"))
                .andExpect(jsonPath("$.data.requiredSkills[0]").value("Java"));
        // identical title/company/location is treated as a duplicate
        mvc.perform(auth(post("/api/v1/admin/jobs"), admin).contentType(MediaType.APPLICATION_JSON).content(json(job)))
                .andExpect(status().isConflict());
        // invalid salary range
        Map<String, Object> bad = jobBody("Other " + tag, "Acme" + tag, "Kandy", "FULL_TIME", "JUNIOR", "HYBRID", "IT", List.of(), 5000, 1000);
        mvc.perform(auth(post("/api/v1/admin/jobs"), admin).contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchFiltersAndPagination() throws Exception {
        String tag = unique();
        String admin = adminToken();
        createJob(admin, jobBody("Java Developer", "Acme" + tag, "Colombo", "FULL_TIME", "JUNIOR", "REMOTE", "Information Technology", List.of("Java", "Spring Boot"), 1000, 2000));
        createJob(admin, jobBody("Accountant", "Acme" + tag, "Kandy", "PART_TIME", "MID", "ON_SITE", "Finance and Accounting", List.of("Accounting"), 3000, 4000));
        createJob(admin, jobBody("Chef", "Acme" + tag, "Galle", "CONTRACT", "SENIOR", "ON_SITE", "Hospitality", List.of("Food Safety"), null, null));
        String user = registerUser();
        mvc.perform(auth(get("/api/v1/jobs").param("q", "acme" + tag), user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("employmentType", "PART_TIME"), user))
                .andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.content[0].title").value("Accountant"));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("location", "colombo"), user))
                .andExpect(jsonPath("$.data.content[0].title").value("Java Developer"));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("workArrangement", "REMOTE"), user))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("experienceLevel", "SENIOR"), user))
                .andExpect(jsonPath("$.data.content[0].title").value("Chef"));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("skill", "spring"), user))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("industry", "finance"), user))
                .andExpect(jsonPath("$.data.content[0].title").value("Accountant"));
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("minSalary", "2500"), user))
                .andExpect(jsonPath("$.data.totalElements").value(2)); // accountant + chef (no salary info)
        mvc.perform(auth(get("/api/v1/jobs").param("company", tag).param("size", "2").param("page", "1").param("sort", "title,asc"), user))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        mvc.perform(auth(get("/api/v1/jobs").param("sort", "doesNotExist,asc"), user)).andExpect(status().isBadRequest());
        mvc.perform(auth(get("/api/v1/jobs/999999"), user)).andExpect(status().isNotFound());
    }

    @Test
    void savingJobsWorksPerUser() throws Exception {
        String admin = adminToken();
        long id = createJob(admin, jobBody("Saver " + unique(), "SaveCo" + unique(), "Colombo", "FULL_TIME", "JUNIOR", "HYBRID", "IT", List.of("Java"), null, null));
        String u1 = registerUser();
        String u2 = registerUser();
        mvc.perform(auth(post("/api/v1/jobs/" + id + "/save"), u1)).andExpect(status().isOk());
        mvc.perform(auth(post("/api/v1/jobs/" + id + "/save"), u1)).andExpect(status().isOk()); // idempotent
        mvc.perform(auth(get("/api/v1/jobs/saved"), u1)).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].saved").value(true));
        mvc.perform(auth(get("/api/v1/jobs/saved"), u2)).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(auth(get("/api/v1/jobs/" + id), u1)).andExpect(jsonPath("$.data.saved").value(true));
        mvc.perform(auth(get("/api/v1/jobs/" + id), u2)).andExpect(jsonPath("$.data.saved").value(false));
        mvc.perform(auth(delete("/api/v1/jobs/" + id + "/save"), u1)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/jobs/saved"), u1)).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(auth(post("/api/v1/jobs/987654/save"), u1)).andExpect(status().isNotFound());
    }

    @Test
    void matchEndpointReturnsMatchingAndMissingSkills() throws Exception {
        String admin = adminToken();
        long id = createJob(admin, jobBody("Match " + unique(), "MatchCo" + unique(), "Colombo", "FULL_TIME", "JUNIOR", "HYBRID", "Information Technology",
                List.of("Java", "Spring Boot", "Kubernetes"), null, null));
        String user = registerUser();
        mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/profile"), user).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fullName", "M U", "yearsOfExperience", 1, "skills", List.of("Java", "Spring Boot"))))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/jobs/" + id + "/match"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matchingSkills[0]").value("Java"))
                .andExpect(jsonPath("$.data.missingSkills[0]").value("Kubernetes"))
                .andExpect(jsonPath("$.data.aiPowered").value(false))
                .andExpect(jsonPath("$.data.score").isNumber())
                .andExpect(jsonPath("$.data.recommendations").isArray());
    }

    @Test
    void recommendedJobsAreRankedByMatchScore() throws Exception {
        String admin = adminToken();
        String tag = unique();
        createJob(admin, jobBody("Rank Low " + tag, "RankCo" + tag, "Colombo", "FULL_TIME", "SENIOR", "ON_SITE", "Hospitality", List.of("Food Safety", "Front Desk"), null, null));
        createJob(admin, jobBody("Rank High " + tag, "RankCo" + tag, "Colombo", "FULL_TIME", "JUNIOR", "ON_SITE", "Information Technology", List.of("Java", "SQL"), null, null));
        String user = registerUser();
        mvc.perform(auth(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/profile"), user).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fullName", "R U", "yearsOfExperience", 1, "skills", List.of("Java", "SQL"))))).andExpect(status().isOk());
        var res = body(mvc.perform(auth(get("/api/v1/jobs/recommended").param("size", "50"), user)).andExpect(status().isOk()).andReturn());
        int high = -1;
        int low = -1;
        for (int i = 0; i < res.at("/data/content").size(); i++) {
            String t = res.at("/data/content/" + i + "/title").asText();
            if (t.equals("Rank High " + tag)) high = i;
            if (t.equals("Rank Low " + tag)) low = i;
        }
        assertTrue(high >= 0 && low >= 0 && high < low, "matching job should rank first");
        assertTrue(res.at("/data/content/" + high + "/matchScore").asInt() > res.at("/data/content/" + low + "/matchScore").asInt());
    }

    @Test
    void externalIngestionDeduplicatesAndExpiresJobs() {
        String tag = unique();
        JobCreateRequest req = new JobCreateRequest("Ingested " + tag, "FeedCo" + tag, null, null, "Remote", "IT", EmploymentType.FULL_TIME,
                WorkArrangement.REMOTE, ExperienceLevel.MID, null, null, null, null, "Description", null, null, null,
                List.of("Java"), List.of(), LocalDate.now(), null, "https://example.com/job", "TESTFEED");
        assertEquals(UpsertOutcome.CREATED, jobService.upsertExternal(req, "TESTFEED", "ext-" + tag));
        assertEquals(UpsertOutcome.UPDATED, jobService.upsertExternal(req, "TESTFEED", "ext-" + tag));
        assertEquals(UpsertOutcome.DUPLICATE, jobService.upsertExternal(req, "OTHERFEED", "other-" + tag));

        JobCreateRequest expired = new JobCreateRequest("Expired " + tag, "FeedCo" + tag, null, null, "Remote", "IT", EmploymentType.FULL_TIME,
                WorkArrangement.REMOTE, ExperienceLevel.MID, null, null, null, null, "Description", null, null, null,
                List.of(), List.of(), LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), null, "TESTFEED");
        assertEquals(UpsertOutcome.CREATED, jobService.upsertExternal(expired, "TESTFEED", "exp-" + tag));
        assertTrue(jobService.expireJobs(List.of(), 21) >= 1);
    }
}
