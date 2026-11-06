package com.careermate.jobs.ingestion;

import com.careermate.config.AppProperties;
import com.careermate.jobs.ExperienceLevel;
import com.careermate.jobs.JobDtos.JobCreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * JSearch by letscrape on RapidAPI (https://rapidapi.com/letscrape-6bRBa3QguO5/api/jsearch).
 * Requires a RapidAPI key; the source is disabled until one is configured.
 */
@Component
public class JSearchJobSource extends AbstractHttpJobSource {
    private final AppProperties props;

    public JSearchJobSource(ObjectMapper mapper, AppProperties props) {
        super(mapper);
        this.props = props;
    }

    @Override
    public String name() {
        return "JSEARCH";
    }

    @Override
    public boolean isEnabled() {
        AppProperties.Sync sync = props.getJobs().getSync();
        return sync.isJsearchEnabled() && hasText(sync.getJsearchApiKey());
    }

    @Override
    public List<ExternalJob> fetch(int max) throws Exception {
        AppProperties.Sync sync = props.getJobs().getSync();
        String host = hasText(sync.getJsearchHost()) ? sync.getJsearchHost().trim() : "jsearch.p.rapidapi.com";
        int numPages = Math.max(1, Math.min(sync.getJsearchNumPages(), 5));
        StringBuilder url = new StringBuilder("https://")
                .append(host)
                .append("/search?query=").append(enc(sync.getJsearchQuery()))
                .append("&page=1&num_pages=").append(numPages);
        if (hasText(sync.getJsearchCountry())) {
            url.append("&country=").append(enc(sync.getJsearchCountry()));
        }
        Map<String, String> headers = Map.of(
                "X-RapidAPI-Key", sync.getJsearchApiKey().trim(),
                "X-RapidAPI-Host", host);
        JsonNode root = getJson(url.toString(), headers);
        List<ExternalJob> out = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            if (out.size() >= max) {
                break;
            }
            String id = n.path("job_id").asText("");
            String title = n.path("job_title").asText("");
            String company = n.path("employer_name").asText("");
            if (id.isBlank() || title.isBlank() || company.isBlank()) {
                continue;
            }
            String description = stripHtml(n.path("job_description").asText(""));
            if (description.isBlank()) {
                description = title + " at " + company;
            }
            String location = joinNonBlank(n.path("job_city").asText(""), n.path("job_state").asText(""),
                    n.path("job_country").asText(""));
            boolean remote = n.path("job_is_remote").asBoolean(false);
            if (location.isBlank()) {
                location = remote ? "Remote" : "Not specified";
            }
            String combined = title + " " + location + " " + description;
            List<String> skills = new ArrayList<>();
            JsonNode requiredSkills = n.path("job_required_skills");
            if (requiredSkills.isArray()) {
                for (JsonNode s : requiredSkills) {
                    if (s.isTextual() && !s.asText().isBlank() && skills.size() < 12) {
                        skills.add(s.asText().trim());
                    }
                }
            }
            for (String s : skillsFrom(combined)) {
                if (skills.size() >= 12) {
                    break;
                }
                if (skills.stream().noneMatch(existing -> existing.equalsIgnoreCase(s))) {
                    skills.add(s);
                }
            }
            JsonNode highlights = n.path("job_highlights");
            String responsibilities = joinArray(highlights.path("Responsibilities"));
            String qualifications = joinArray(highlights.path("Qualifications"));
            String salaryPeriod = n.path("job_salary_period").asText("");
            BigDecimal salaryMin = annualize(decimalOrNull(n, "job_min_salary"), salaryPeriod);
            BigDecimal salaryMax = annualize(decimalOrNull(n, "job_max_salary"), salaryPeriod);
            out.add(new ExternalJob(id, new JobCreateRequest(
                    title, company, blankToNull(n.path("employer_logo").asText("")), null,
                    location, null,
                    inferType(n.path("job_employment_type").asText("")), inferArrangement(combined, remote),
                    levelFromMonths(n.path("job_experience_in_months"), title),
                    salaryMin, salaryMax,
                    blankToNull(n.path("job_salary_currency").asText("")),
                    truncate(description, 600), description,
                    blankToNull(responsibilities), blankToNull(qualifications), null,
                    skills, List.of(),
                    parseDate(n.path("job_posted_at_datetime_utc").asText("")), null,
                    blankToNull(n.path("job_apply_link").asText("")), name())));
        }
        return out;
    }

    private static ExperienceLevel levelFromMonths(JsonNode months, String title) {
        if (months.isNumber()) {
            int m = months.asInt();
            if (m > 0) {
                if (m < 12) return ExperienceLevel.ENTRY;
                if (m < 24) return ExperienceLevel.JUNIOR;
                if (m < 60) return ExperienceLevel.MID;
                if (m < 120) return ExperienceLevel.SENIOR;
                return ExperienceLevel.EXECUTIVE;
            }
        }
        return inferLevel(title);
    }

    private static BigDecimal decimalOrNull(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isNumber() ? v.decimalValue() : null;
    }

    /** JSearch reports pay per year/month/week/day/hour; the UI shows annual amounts. */
    private static BigDecimal annualize(BigDecimal value, String period) {
        if (value == null) {
            return null;
        }
        String p = period == null ? "" : period.toUpperCase(java.util.Locale.ROOT);
        int factor = switch (p) {
            case "MONTH" -> 12;
            case "WEEK" -> 52;
            case "DAY" -> 260;
            case "HOUR" -> 2080;
            default -> 1;
        };
        return value.multiply(BigDecimal.valueOf(factor));
    }

    private static String joinArray(JsonNode arr) {
        if (!arr.isArray()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JsonNode e : arr) {
            if (e.isTextual() && !e.asText().isBlank() && parts.size() < 20) {
                parts.add(stripHtml(e.asText()));
            }
        }
        return truncate(String.join("\n", parts), 8000);
    }

    private static String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                parts.add(v.trim());
            }
        }
        return String.join(", ", parts);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
