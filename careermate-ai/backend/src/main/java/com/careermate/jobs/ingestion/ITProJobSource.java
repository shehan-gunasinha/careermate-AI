package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ITPro.lk public job widget API (https://itpro.lk/developer/). Keyless; the endpoint returns a
 * JavaScript assignment ({@code var jobs = [...]}) rather than raw JSON, and links back to the original
 * posting. Sync on a schedule and respect the source site when re-publishing.
 */
@Component
public class ITProJobSource extends AbstractHttpJobSource {
    private final AppProperties props;

    public ITProJobSource(ObjectMapper mapper, AppProperties props) {
        super(mapper);
        this.props = props;
    }

    @Override
    public String name() {
        return "ITPRO";
    }

    @Override
    public boolean isEnabled() {
        return props.getJobs().getSync().isItproEnabled();
    }

    @Override
    public List<ExternalJob> fetch(int max) throws Exception {
        AppProperties.Sync sync = props.getJobs().getSync();
        int count = Math.max(1, Math.min(max, sync.getItproCount()));
        String url = "https://itpro.lk/api?action=getJobs"
                + "&type=" + urlPart(sync.getItproType(), "0")
                + "&category=" + urlPart(sync.getItproCategory(), "0")
                + "&count=" + count
                + "&random=0"
                + "&days_behind=" + Math.max(1, sync.getItproDaysBehind())
                + "&response=json";
        return mapJobs(getJsonFromScriptArray(url), max);
    }

    List<ExternalJob> mapJobs(JsonNode jobs, int max) {
        List<ExternalJob> out = new ArrayList<>();
        if (jobs == null || !jobs.isArray()) {
            return out;
        }
        for (JsonNode n : jobs) {
            if (out.size() >= max) {
                break;
            }
            String id = text(n, "id");
            String title = text(n, "title");
            if (id.isBlank() || title.isBlank()) {
                continue;
            }
            String company = text(n, "company");
            if (company.isBlank()) {
                company = "Confidential";
            }
            String description = stripHtml(text(n, "description"));
            if (description.isBlank()) {
                description = title + " at " + company;
            }
            String rawLocation = text(n, "location");
            String type = text(n, "type_var_name");
            if (type.isBlank()) {
                type = text(n, "type_name");
            }
            String category = text(n, "category_name");
            String urlTitle = text(n, "url_title");
            String link = urlTitle.isBlank()
                    ? "https://itpro.lk/job/" + id
                    : "https://itpro.lk/job/" + id + "/" + urlTitle + "/";
            String combined = title + " " + description;
            out.add(new ExternalJob(id, new JobCreateRequest(
                    title, company, null, null,
                    rawLocation.isBlank() ? "Sri Lanka" : rawLocation,
                    category.isBlank() ? "Information Technology" : category,
                    inferType(type), inferArrangement(combined, false), inferLevel(title),
                    null, null, null,
                    truncate(description, 600), truncate(description, 15000),
                    null, null, null,
                    new ArrayList<>(skillsFrom(combined)), List.of(),
                    parseDate(text(n, "created_on")), null,
                    link, name())));
        }
        return out;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isNull() || v.isMissingNode() ? "" : v.asText("").trim();
    }

    private static String urlPart(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
