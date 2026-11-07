package com.careermate;

import com.careermate.ai.GeminiClient;
import com.careermate.ai.GeminiKeyService;
import com.careermate.common.exception.AiUnavailableException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantIntegrationTest extends IntegrationTestBase {
    @MockBean GeminiClient gemini;
    @MockBean GeminiKeyService geminiKeys;

    private String ask(String token, String message, Long conversationId) throws Exception {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("message", message);
        if (conversationId != null) m.put("conversationId", conversationId);
        return mvc.perform(auth(post("/api/v1/career-assistant"), token).contentType(MediaType.APPLICATION_JSON).content(json(m)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void fallbackAnswersWhenNoApiKeyIsConfigured() throws Exception {
        when(geminiKeys.resolveKey(any())).thenReturn(null);
        when(geminiKeys.isConfigured(any())).thenReturn(false);
        String token = registerUser();
        var r = mapper.readTree(ask(token, "Give me common interview questions for an accountant", null));
        assertTrue(r.at("/data/aiPowered").asBoolean() == false);
        assertTrue(r.at("/data/reply").asText().contains("accrual"));
        long conv = r.at("/data/conversationId").asLong();
        ask(token, "How should I answer salary expectation questions?", conv);
        mvc.perform(auth(get("/api/v1/career-assistant/conversations"), token)).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(auth(get("/api/v1/career-assistant/conversations/" + conv + "/messages"), token))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"));
        mvc.perform(auth(get("/api/v1/career-assistant/status"), token)).andExpect(jsonPath("$.data.mode").value("FALLBACK"));
    }

    @Test
    void usesGeminiWhenAvailableAndFallsBackOnFailure() throws Exception {
        when(geminiKeys.resolveKey(any())).thenReturn("test-key");
        when(geminiKeys.isConfigured(any())).thenReturn(true);
        when(gemini.chat(eq("test-key"), anyString(), anyList())).thenReturn("Here is AI advice.");
        String token = registerUser();
        var ai = mapper.readTree(ask(token, "Help me plan my career", null));
        assertTrue(ai.at("/data/aiPowered").asBoolean());
        assertTrue(ai.at("/data/reply").asText().equals("Here is AI advice."));

        when(gemini.chat(eq("test-key"), anyString(), anyList())).thenThrow(new AiUnavailableException("rate limited"));
        var fb = mapper.readTree(ask(token, "How can I improve my resume?", ai.at("/data/conversationId").asLong()));
        assertTrue(!fb.at("/data/aiPowered").asBoolean());
        assertTrue(fb.at("/data/reply").asText().toLowerCase().contains("resume"));
    }

    @Test
    void conversationsArePrivateAndCanBeCleared() throws Exception {
        when(geminiKeys.resolveKey(any())).thenReturn(null);
        when(geminiKeys.isConfigured(any())).thenReturn(false);
        String owner = registerUser();
        String other = registerUser();
        long conv = mapper.readTree(ask(owner, "hello", null)).at("/data/conversationId").asLong();
        mvc.perform(auth(get("/api/v1/career-assistant/conversations/" + conv + "/messages"), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/v1/career-assistant/conversations/" + conv), other)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/v1/career-assistant"), other).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("message", "hi", "conversationId", conv)))).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/v1/career-assistant/conversations"), other)).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(auth(delete("/api/v1/career-assistant/conversations/" + conv), owner)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/career-assistant/conversations"), owner)).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void emptyMessagesAreRejected() throws Exception {
        String token = registerUser();
        mvc.perform(auth(post("/api/v1/career-assistant"), token).contentType(MediaType.APPLICATION_JSON).content(json(Map.of("message", "  "))))
                .andExpect(status().isBadRequest());
    }
}
