package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.careermate.jobs.WorkArrangement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Arbeitnow free job board API (https://www.arbeitnow.com/api/job-board-api). */
@Component
public class ArbeitnowJobSource extends AbstractHttpJobSource {
    private final AppProperties props;

    public ArbeitnowJobSource(ObjectMapper mapper, AppProperties props) {
        super(mapper);
        this.props = props;
    }

    @Override
    public String name() {
        return "ARBEITNOW";
    }

    @Override
    public boolean isEnabled() {
        return props.getJobs().getSync().isArbeitnowEnabled();
    }

    @Override
    public List<ExternalJob> fetch(int max) throws Exception {
        JsonNode root = getJson("https://www.arbeitnow.com/api/job-board-api");
        List<ExternalJob> out = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            if (out.size() >= max) {
                break;
            }
            String id = n.path("slug").asText("");
            String title = n.path("title").asText("");
            String company = n.path("company_name").asText("");
            if (id.isBlank() || title.isBlank() || company.isBlank()) {
                continue;
            }
            String desc = stripHtml(n.path("description").asText(""));
            if (desc.isBlank()) {
                desc = title + " at " + company;
            }
            LocalDate posted = n.path("created_at").canConvertToLong()
                    ? Instant.ofEpochSecond(n.path("created_at").asLong()).atZone(ZoneOffset.UTC).toLocalDate() : null;
            String typeRaw = n.path("job_types").isArray() && n.path("job_types").size() > 0 ? n.path("job_types").get(0).asText("") : "";
            String location = n.path("location").asText("");
            boolean remote = n.path("remote").asBoolean(false);
            List<String> skills = new ArrayList<>(skillsFrom(title + " " + desc));
            out.add(new ExternalJob(id, new JobCreateRequest(title, company, null, null,
                    location.isBlank() ? (remote ? "Remote" : "Germany") : location, null,
                    inferType(typeRaw), remote ? WorkArrangement.REMOTE : WorkArrangement.ON_SITE, inferLevel(title),
                    null, null, null, null, desc, null, null, null, skills, List.of(), posted, null,
                    n.path("url").asText(null), name())));
        }
        return out;
    }
}
