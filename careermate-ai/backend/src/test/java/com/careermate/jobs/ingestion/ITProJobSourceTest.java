package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.EmploymentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ITProJobSourceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ITProJobSource source = new ITProJobSource(mapper, new AppProperties());

    @Test
    void mapsWidgetJsonIntoNormalisedJobs() throws Exception {
        var root = mapper.readTree("""
                [{"id":"15706","title":"Java Developer","description":"<p>Build REST APIs with Java, Spring Boot and SQL.</p>","company":"Acme","location":"Colombo","type_name":"Full-time","type_var_name":"fulltime","category_name":"Software Engineering","url_title":"java-developer-at-acme","created_on":"2026-11-06 18:33:14"}]
                """);
        List<ExternalJobSource.ExternalJob> jobs = source.mapJobs(root, 10);
        assertEquals(1, jobs.size());
        ExternalJobSource.ExternalJob job = jobs.get(0);
        assertEquals("15706", job.externalId());
        assertEquals("ITPRO", job.job().source());
        assertEquals("Acme", job.job().companyName());
        assertEquals("Colombo", job.job().location());
        assertEquals(EmploymentType.FULL_TIME, job.job().employmentType());
        assertEquals(LocalDate.of(2026, 11, 6), job.job().postedDate());
        assertEquals("https://itpro.lk/job/15706/java-developer-at-acme/", job.job().applicationUrl());
        assertTrue(job.job().requiredSkills().containsAll(List.of("Java", "Spring Boot", "SQL")),
                "skills " + job.job().requiredSkills());
    }
}
