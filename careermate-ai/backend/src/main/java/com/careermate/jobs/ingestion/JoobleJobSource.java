package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Jooble aggregator API (https://jooble.org/api/about). Requires a free API key.
 * The key is sent in the URL path, so the source is disabled until one is configured.
 */
@Component
public class JoobleJobSource extends AbstractHttpJobSource {
    private static final int RESULTS_PER_PAGE = 20;

    private final AppProperties props;

    public JoobleJobSource(ObjectMapper mapper, AppProperties props) {
        super(mapper);
        this.props = props;
    }

    @Override
    public String name() {
        return "JOOBLE";
    }

    @Override
    public boolean isEnabled() {
        AppProperties.Sync sync = props.getJobs().getSync();
        return sync.isJoobleEnabled() && hasText(sync.getJoobleApiKey());
    }

    @Override
    public List<ExternalJob> fetch(int max) throws Exception {
        AppProperties.Sync sync = props.getJobs().getSync();
        String url = "https://jooble.org/api/" + sync.getJoobleApiKey().trim();
        int pagesWanted = (max + RESULTS_PER_PAGE - 1) / RESULTS_PER_PAGE;
        int maxPages = Math.max(1, Math.min(sync.getJoobleMaxPages(), pagesWanted));
        List<ExternalJob> out = new ArrayList<>();
        for (int page = 1; page <= maxPages && out.size() < max; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("keywords", sync.getJoobleKeywords());
            body.put("page", String.valueOf(page));
            if (hasText(sync.getJoobleLocation())) {
                body.put("location", sync.getJoobleLocation());
            }
            JsonNode root = postJson(url, Map.of(), mapper.writeValueAsString(body));
            JsonNode jobs = root.path("jobs");
            if (!jobs.isArray() || jobs.isEmpty()) {
                break;
            }
            for (JsonNode n : jobs) {
                if (out.size() >= max) {
                    break;
                }
                String id = n.path("id").asText("");
                String title = n.path("title").asText("");
                String company = n.path("company").asText("");
                String link = n.path("link").asText("");
                if (id.isBlank() || title.isBlank() || company.isBlank()) {
                    continue;
                }
                String location = n.path("location").asText("");
                String snippet = stripHtml(n.path("snippet").asText(""));
                String description = snippet.isBlank() ? title + " at " + company : snippet;
                String combined = title + " " + location + " " + description;
                List<String> skills = new ArrayList<>(skillsFrom(combined));
                out.add(new ExternalJob(id, new JobCreateRequest(
                        title, company, null, null,
                        location.isBlank() ? "Not specified" : location, null,
                        inferType(n.path("type").asText("")), inferArrangement(combined, false), inferLevel(title),
                        null, null, null, truncate(description, 600), description,
                        null, null, null, skills, List.of(),
                        parseDate(n.path("updated").asText("")), null,
                        link.isBlank() ? null : link, name())));
            }
        }
        return out;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
