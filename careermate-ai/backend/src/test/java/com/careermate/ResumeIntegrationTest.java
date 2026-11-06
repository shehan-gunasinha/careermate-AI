package com.careermate;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeIntegrationTest extends IntegrationTestBase {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private long upload(String token, String name, String type, byte[] bytes) throws Exception {
        return body(mvc.perform(auth(multipart("/api/v1/resumes/upload").file(new MockMultipartFile("file", name, type, bytes)), token))
                .andExpect(status().isCreated()).andReturn()).at("/data/id").asLong();
    }

    @Test
    void uploadDocxExtractsTextAndAnalysisFallbackWorks() throws Exception {
        String token = registerUser();
        long id = upload(token, "jane-cv.docx", DOCX, docx(SAMPLE_RESUME));
        mvc.perform(auth(get("/api/v1/resumes/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedInfo.email").value("jane.sample@example.com"))
                .andExpect(jsonPath("$.data.extractedInfo.detectedSkills").isNotEmpty())
                .andExpect(jsonPath("$.data.extractedText").value(org.hamcrest.Matchers.containsString("Spring Boot")));
        mvc.perform(auth(get("/api/v1/resumes/" + id + "/analysis"), token)).andExpect(status().isNotFound());
        var analysis = body(mvc.perform(auth(post("/api/v1/resumes/" + id + "/analyze"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiPowered").value(false)).andReturn());
        int score = analysis.at("/data/overallScore").asInt();
        assertTrue(score > 50 && score <= 100, "unexpected score " + score);
        assertEquals(9, analysis.at("/data/sectionScores").size());
        mvc.perform(auth(get("/api/v1/resumes/" + id + "/analysis"), token)).andExpect(jsonPath("$.data.overallScore").value(score));
        mvc.perform(auth(post("/api/v1/resumes/" + id + "/analyze"), token)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/resumes/" + id + "/analyses"), token)).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(auth(get("/api/v1/resumes"), token)).andExpect(jsonPath("$.data[0].latestScore").value(score));
    }

    @Test
    void uploadPdfWorksAndDownloadReturnsSameFile() throws Exception {
        String token = registerUser();
        byte[] pdf = pdf(SAMPLE_RESUME);
        long id = upload(token, "cv.pdf", "application/pdf", pdf);
        byte[] downloaded = mvc.perform(auth(get("/api/v1/resumes/" + id + "/download"), token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals(pdf.length, downloaded.length);
        mvc.perform(auth(get("/api/v1/resumes/" + id), token)).andExpect(jsonPath("$.data.extractedInfo.wordCount").isNumber());
    }

    @Test
    void invalidUploadsAreRejected() throws Exception {
        String token = registerUser();
        mvc.perform(auth(multipart("/api/v1/resumes/upload").file(new MockMultipartFile("file", "cv.txt", "text/plain", "hello".getBytes())), token))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(multipart("/api/v1/resumes/upload").file(new MockMultipartFile("file", "cv.pdf", "application/pdf", "not a pdf at all".getBytes())), token))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(multipart("/api/v1/resumes/upload").file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0])), token))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/resumes/upload").file(new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf("x"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resumesArePrivateToTheirOwner() throws Exception {
        String owner = registerUser();
        String other = registerUser();
        long id = upload(owner, "private.docx", DOCX, docx(SAMPLE_RESUME));
        mvc.perform(auth(get("/api/v1/resumes/" + id), other)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/resumes/" + id + "/download"), other)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/v1/resumes/" + id + "/analyze"), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/v1/resumes/" + id), other)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/resumes"), other)).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(auth(delete("/api/v1/resumes/" + id), owner)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/resumes/" + id), owner)).andExpect(status().isNotFound());
    }

    @Test
    void resumeBuilderCreatesDownloadablePdf() throws Exception {
        String token = registerUser();
        Map<String, Object> req = Map.of("title", "My Builder CV", "template", "modern",
                "personal", Map.of("fullName", "Sam Builder", "email", "sam@example.com", "phone", "0771234567", "headline", "Accountant"),
                "summary", "Accountant with experience in month-end reporting.",
                "experience", List.of(Map.of("company", "Sample Ltd", "title", "Accountant", "startDate", "Jan 2021", "current", true,
                        "description", "Prepared monthly reports\nReconciled ledgers")),
                "education", List.of(Map.of("institution", "Sample Uni", "degree", "BCom", "period", "2017-2020")),
                "skills", List.of("Excel", "IFRS"),
                "certifications", List.of(Map.of("name", "Sample Cert", "year", "2022")),
                "achievements", List.of("Reduced close time"));
        long id = body(mvc.perform(auth(post("/api/v1/resumes/build"), token).contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.source").value("BUILT")).andReturn()).at("/data/id").asLong();
        byte[] pdf = mvc.perform(auth(get("/api/v1/resumes/" + id + "/download"), token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(pdf, 0, 4));
        mvc.perform(auth(post("/api/v1/resumes/" + id + "/analyze"), token)).andExpect(status().isOk());
        mvc.perform(auth(post("/api/v1/resumes/build"), token).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("title", "x", "personal", Map.of("fullName", ""))))).andExpect(status().isBadRequest());
    }

    @Test
    void suggestionFallsBackWithoutAiKey() throws Exception {
        String token = registerUser();
        mvc.perform(auth(post("/api/v1/resumes/suggest"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("section", "summary", "content", "I am a hard worker with experience in sales."))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.aiPowered").value(false))
                .andExpect(jsonPath("$.data.suggestion").isNotEmpty());
    }
}
