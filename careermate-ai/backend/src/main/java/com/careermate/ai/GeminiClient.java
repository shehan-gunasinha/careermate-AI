package com.careermate.ai;

import com.careermate.common.exception.AiUnavailableException;
import com.careermate.common.exception.BadRequestException;
import com.careermate.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around the Google Gemini REST API. API keys are supplied by the backend only and are sent
 * in a request header, never exposed to the browser.
 */
@Slf4j
@Component
public class GeminiClient {
    public record Turn(String role, String text) {
        public static Turn user(String text) {
            return new Turn("user", text);
        }

        public static Turn model(String text) {
            return new Turn("model", text);
        }
    }

    private final AppProperties.Gemini cfg;
    private final ObjectMapper mapper;
    private final HttpClient http;

    public GeminiClient(AppProperties props, ObjectMapper mapper) {
        this.cfg = props.getGemini();
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** Whether the shared server-side key is configured. */
    public boolean isConfigured() {
        return cfg.getApiKey() != null && !cfg.getApiKey().isBlank();
    }

    /** Shared server-side key, read only by backend services. */
    public String serverApiKey() {
        return cfg.getApiKey();
    }

    /** Verifies that Google accepts the supplied key without generating content or exposing the key. */
    public void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("Enter your Gemini API key");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.getBaseUrl() + "/models?pageSize=1"))
                    .timeout(Duration.ofSeconds(Math.min(cfg.getTimeoutSeconds(), 15)))
                    .header("x-goog-api-key", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 429) {
                return;
            }
            if (resp.statusCode() == 400 || resp.statusCode() == 401 || resp.statusCode() == 403) {
                throw new BadRequestException("That Gemini API key was rejected by Google. Please check it and try again.");
            }
            if (resp.statusCode() >= 500) {
                throw new AiUnavailableException("Google could not verify the API key right now. Please try again shortly.");
            }
            if (resp.statusCode() >= 400) {
                throw new AiUnavailableException("Could not verify the API key with the configured Gemini endpoint.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("API key verification was interrupted", e);
        } catch (IOException e) {
            throw new AiUnavailableException("Could not reach Google to verify the API key. Please try again.", e);
        } catch (IllegalArgumentException e) {
            throw new AiUnavailableException("Gemini is misconfigured", e);
        }
    }

    /** Multi-turn chat; returns plain text. */
    public String chat(String apiKey, String systemInstruction, List<Turn> turns) {
        return call(apiKey, systemInstruction, turns, false);
    }

    /** Single prompt that must return a JSON object. */
    public JsonNode generateJson(String apiKey, String systemInstruction, String prompt) {
        String raw = call(apiKey, systemInstruction, List.of(Turn.user(prompt)), true);
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```\\s*$", "").trim();
        }
        try {
            JsonNode node = mapper.readTree(cleaned);
            if (node == null || !node.isObject()) {
                throw new AiUnavailableException("AI returned an unexpected response format");
            }
            return node;
        } catch (IOException e) {
            throw new AiUnavailableException("AI returned an invalid JSON response", e);
        }
    }

    private String call(String apiKey, String systemInstruction, List<Turn> turns, boolean json) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiUnavailableException("No Gemini API key is configured for this account");
        }
        ObjectNode body = mapper.createObjectNode();
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", systemInstruction);
        }
        ArrayNode contents = body.putArray("contents");
        for (Turn t : turns) {
            ObjectNode c = contents.addObject();
            c.put("role", t.role());
            c.putArray("parts").addObject().put("text", t.text());
        }
        ObjectNode gen = body.putObject("generationConfig");
        gen.put("temperature", json ? 0.2 : 0.7);
        if (json) {
            gen.put("responseMimeType", "application/json");
        }
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.getBaseUrl() + "/models/" + cfg.getModel() + ":generateContent"))
                    .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 429) {
                throw new AiUnavailableException("AI service rate limit reached, please try again shortly");
            }
            if (resp.statusCode() >= 400) {
                log.warn("Gemini returned HTTP {}", resp.statusCode());
                throw new AiUnavailableException("AI service returned an error (HTTP " + resp.statusCode() + ")");
            }
            JsonNode root = mapper.readTree(resp.body());
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
                if (part.hasNonNull("text")) {
                    sb.append(part.get("text").asText());
                }
            }
            if (sb.toString().isBlank()) {
                throw new AiUnavailableException("AI service returned an empty response");
            }
            return sb.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("AI request was interrupted", e);
        } catch (IOException e) {
            throw new AiUnavailableException("AI service is unreachable or timed out", e);
        } catch (IllegalArgumentException e) {
            throw new AiUnavailableException("AI service is misconfigured", e);
        }
    }
}
