package com.careermate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careermate.user.Role;
import com.careermate.user.User;
import com.careermate.user.UserRepository;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
    protected static final String PASSWORD = "Passw0rdTest";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    protected String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    protected static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    /** Registers a new job seeker through the public API and returns its JWT. */
    protected String registerUser(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("fullName", "Test User", "email", email, "password", PASSWORD, "confirmPassword", PASSWORD))))
                .andReturn();
        return body(r).at("/data/token").asText();
    }

    protected String registerUser() throws Exception {
        return registerUser("user-" + unique() + "@test.local");
    }

    /** Creates an admin directly in the database and logs in through the API. */
    protected String adminToken() throws Exception {
        String email = "admin-" + unique() + "@test.local";
        User u = new User();
        u.setFullName("Admin");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setRole(Role.ADMIN);
        userRepository.save(u);
        MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", PASSWORD)))).andReturn();
        return body(r).at("/data/token").asText();
    }

    protected Map<String, Object> jobBody(String title, String company, String location, String type, String level,
                                          String arrangement, String industry, List<String> skills, Integer salaryMin, Integer salaryMax) {
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put("title", title);
        m.put("companyName", company);
        m.put("location", location);
        m.put("industry", industry);
        m.put("employmentType", type);
        m.put("workArrangement", arrangement);
        m.put("experienceLevel", level);
        m.put("description", "[DEMO] " + title + " role at " + company + " for testing.");
        m.put("requiredSkills", skills);
        if (salaryMin != null) m.put("salaryMin", salaryMin);
        if (salaryMax != null) m.put("salaryMax", salaryMax);
        return m;
    }

    protected long createJob(String adminToken, Map<String, Object> body) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/v1/admin/jobs"), adminToken).contentType(MediaType.APPLICATION_JSON).content(json(body))).andReturn();
        return body(r).at("/data/id").asLong();
    }

    protected static byte[] docx(String... paragraphs) throws Exception {
        try (XWPFDocument d = new XWPFDocument(); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            for (String p : paragraphs) {
                d.createParagraph().createRun().setText(p);
            }
            d.write(o);
            return o.toByteArray();
        }
    }

    protected static byte[] pdf(String... lines) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, o);
        doc.open();
        for (String l : lines) {
            doc.add(new Paragraph(l));
        }
        doc.close();
        return o.toByteArray();
    }

    protected static final String[] SAMPLE_RESUME = {
            "Jane Sample", "jane.sample@example.com | +94 77 123 4567 | linkedin.com/in/janesample",
            "Professional Summary", "Software engineer with experience building Java and Spring Boot REST APIs and SQL databases for web applications.",
            "Experience", "Software Engineer, Sample Ltd 2022 - 2024",
            "- Developed REST APIs with Java and Spring Boot used by 5000 users",
            "- Improved query performance by 30% using SQL indexing",
            "- Built CI/CD pipelines with Docker and Git",
            "- Led code reviews and mentored two interns",
            "- Implemented automated testing that reduced defects",
            "Education", "BSc (Hons) in Computer Science, Sample University 2018 - 2022",
            "Skills", "Java, Spring Boot, SQL, REST APIs, Docker, Git, PostgreSQL, Agile",
            "Certifications", "AWS Certified Cloud Practitioner",
            "Achievements", "Won university hackathon 2021"};
}
