package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.careermate.jobs.WorkArrangement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Remotive public API (https://remotive.com/api/remote-jobs). Remotive asks consumers to link back to the original
 * posting and not to poll more than a few times a day; we store the source URL and sync on a schedule.
 */
@Component
public class RemotiveJobSource extends AbstractHttpJobSource {
    private final AppProperties props;

    public RemotiveJobSource(ObjectMapper mapper, AppProperties props) {
        super(mapper);
        this.props = props;
    }

    @Override
    public String name() {
        return "REMOTIVE";
    }

    @Override
    public boolean isEnabled() {
        return props.getJobs().getSync().isRemotiveEnabled();
    }

    @Override
    public List<ExternalJob> fetch(int max) throws Exception {
        JsonNode root = getJson("https://remotive.com/api/remote-jobs?limit=" + max);
        List<ExternalJob> out = new ArrayList<>();
        for (JsonNode n : root.path("jobs")) {
            if (out.size() >= max) {
                break;
            }
            String id = n.path("id").asText("");
            String title = n.path("title").asText("");
            String company = n.path("company_name").asText("");
            String url = n.path("url").asText("");
            if (id.isBlank() || title.isBlank() || company.isBlank()) {
                continue;
            }
            String desc = stripHtml(n.path("description").asText(""));
            if (desc.isBlank()) {
                desc = title + " at " + company;
            }
            LocalDate posted = null;
            String pub = n.path("publication_date").asText("");
            if (pub.length() >= 10) {
                try {
                    posted = LocalDate.parse(pub.substring(0, 10));
                } catch (Exception ignored) {
                    // keep null -> defaults to today
                }
            }
            String location = n.path("candidate_required_location").asText("Worldwide");
            List<String> skills = new ArrayList<>(skillsFrom(title + " " + desc));
            n.path("tags").forEach(t -> {
                if (t.isTextual() && skills.size() < 12 && skills.stream().noneMatch(s -> s.equalsIgnoreCase(t.asText()))) {
                    skills.add(t.asText());
                }
            });
            out.add(new ExternalJob(id, new JobCreateRequest(title, company, blankToNull(n.path("company_logo").asText("")), null,
                    location.isBlank() ? "Worldwide" : location, n.path("category").asText(null),
                    inferType(n.path("job_type").asText("")), WorkArrangement.REMOTE, inferLevel(title),
                    null, null, null, null, desc, null, null, null, skills, List.of(), posted, null,
                    blankToNull(url), name())));
        }
        return out;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
