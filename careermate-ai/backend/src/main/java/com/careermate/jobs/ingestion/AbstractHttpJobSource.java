package com.careermate.jobs.ingestion;

import com.careermate.common.util.SkillCatalog;
import com.careermate.jobs.ExperienceLevel;
import com.careermate.jobs.EmploymentType;
import com.careermate.jobs.WorkArrangement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class AbstractHttpJobSource implements ExternalJobSource {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    protected final ObjectMapper mapper;

    protected AbstractHttpJobSource(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    protected JsonNode getJson(String url) throws IOException, InterruptedException {
        return getJson(url, Map.of());
    }

    protected JsonNode getJson(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json").header("User-Agent", "CareerMateAI/1.0").GET();
        headers.forEach(builder::header);
        return send(builder.build(), url);
    }

    protected JsonNode postJson(String url, Map<String, String> headers, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json").header("Content-Type", "application/json")
                .header("User-Agent", "CareerMateAI/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return send(builder.build(), url);
    }

    /**
     * Some job boards return a JavaScript assignment such as {@code var jobs = [...];}.
     * Extracts the JSON array before parsing.
     */
    protected JsonNode getJsonFromScriptArray(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json").header("User-Agent", "CareerMateAI/1.0").GET().build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IOException("HTTP " + resp.statusCode() + " from " + url);
        }
        String body = resp.body() == null ? "" : resp.body().trim();
        int start = body.indexOf('[');
        int end = body.lastIndexOf(']');
        if (start >= 0 && end > start) {
            body = body.substring(start, end + 1);
        }
        return mapper.readTree(body);
    }

    private JsonNode send(HttpRequest request, String url) throws IOException, InterruptedException {
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new IOException("HTTP " + resp.statusCode() + " from " + url);
        }
        return mapper.readTree(resp.body());
    }

    protected static String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("(?i)<br\\s*/?>|</p>|</li>", "\n").replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
        text = text.replaceAll("[ \\t]+", " ").replaceAll("\\n\\s*\\n+", "\n\n").trim();
        return text.length() > 15000 ? text.substring(0, 15000) : text;
    }

    protected static List<String> skillsFrom(String text) {
        List<String> s = SkillCatalog.findIn(text);
        return s.size() > 12 ? s.subList(0, 12) : s;
    }

    protected static ExperienceLevel inferLevel(String title) {
        String t = title.toLowerCase(Locale.ROOT);
        if (t.matches(".*\\b(chief|vp|director|head of)\\b.*")) return ExperienceLevel.EXECUTIVE;
        if (t.matches(".*\\b(senior|sr|lead|principal|staff|manager)\\b.*")) return ExperienceLevel.SENIOR;
        if (t.matches(".*\\b(intern|trainee|graduate|entry)\\b.*")) return ExperienceLevel.ENTRY;
        if (t.matches(".*\\b(junior|jr|associate)\\b.*")) return ExperienceLevel.JUNIOR;
        return ExperienceLevel.MID;
    }

    protected static EmploymentType inferType(String raw) {
        String t = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if (t.contains("part")) return EmploymentType.PART_TIME;
        if (t.contains("intern")) return EmploymentType.INTERNSHIP;
        if (t.contains("contract")) return EmploymentType.CONTRACT;
        if (t.contains("freelance")) return EmploymentType.FREELANCE;
        if (t.contains("temp")) return EmploymentType.TEMPORARY;
        return EmploymentType.FULL_TIME;
    }

    protected static WorkArrangement inferArrangement(String text, boolean remote) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (remote || t.contains("remote") || t.contains("work from home") || t.contains("telecommute")) {
            return WorkArrangement.REMOTE;
        }
        if (t.contains("hybrid")) {
            return WorkArrangement.HYBRID;
        }
        return WorkArrangement.ON_SITE;
    }

    /** Parses the leading YYYY-MM-DD from an ISO-ish timestamp, returning null when unavailable. */
    protected static LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception ignored) {
            return null;
        }
    }

    protected static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
