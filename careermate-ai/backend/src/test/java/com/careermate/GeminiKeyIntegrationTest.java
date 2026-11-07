package com.careermate;

import com.careermate.ai.GeminiClient;
import com.careermate.common.exception.BadRequestException;
import com.careermate.user.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeminiKeyIntegrationTest extends IntegrationTestBase {
    @MockBean GeminiClient gemini;

    @Test
    void savesReplacesAndRemovesKeyWithoutExposingIt() throws Exception {
        when(gemini.isConfigured()).thenReturn(false);
        String email = "key-" + unique() + "@test.local";
        String token = registerUser(email);

        mvc.perform(auth(get("/api/v1/ai/gemini-key"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.serverKeyConfigured").value(false))
                .andExpect(jsonPath("$.data.aiEnabled").value(false));

        String firstKey = "first-test-key-1234567890";
        var saved = mvc.perform(auth(put("/api/v1/ai/gemini-key"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("apiKey", firstKey))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.hint").value("****7890"))
                .andExpect(jsonPath("$.data.aiEnabled").value(true))
                .andReturn();
        assertFalse(saved.getResponse().getContentAsString().contains(firstKey));

        User stored = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotEquals(firstKey, stored.getGeminiApiKeyEncrypted());
        assertTrue(stored.getGeminiApiKeyEncrypted().startsWith("v1:"));

        String secondKey = "second-test-key-1234561111";
        mvc.perform(auth(put("/api/v1/ai/gemini-key"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("apiKey", secondKey))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hint").value("****1111"));

        mvc.perform(auth(delete("/api/v1/ai/gemini-key"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.aiEnabled").value(false));

        User cleared = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNull(cleared.getGeminiApiKeyEncrypted());
    }

    @Test
    void rejectsKeysGoogleDoesNotAccept() throws Exception {
        when(gemini.isConfigured()).thenReturn(false);
        doThrow(new BadRequestException("That Gemini API key was rejected by Google. Please check it and try again."))
                .when(gemini).validateApiKey(anyString());
        String token = registerUser();

        mvc.perform(auth(put("/api/v1/ai/gemini-key"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("apiKey", "definitely-not-a-valid-key"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("That Gemini API key was rejected by Google. Please check it and try again."));
    }
}
