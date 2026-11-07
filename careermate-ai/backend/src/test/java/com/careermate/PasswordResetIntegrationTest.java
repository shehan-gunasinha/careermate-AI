package com.careermate;

import com.careermate.common.mail.EmailService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIntegrationTest extends IntegrationTestBase {

    @MockBean
    private EmailService emailService;

    @Test
    void forgotPasswordEmailsCodeAndResetChangesThePassword() throws Exception {
        String email = "reset-" + unique() + "@test.local";
        registerUser(email);

        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetCode(eq(email), anyString(), code.capture(), eq(10));
        assertEquals(6, code.getValue().length());

        String newPassword = "NewPassw0rd";
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "otp", code.getValue(),
                                "newPassword", newPassword, "confirmPassword", newPassword))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", newPassword))))
                .andExpect(status().isOk());
        assertNotEquals(code.getValue(), "000000", "a random six-digit code is expected");
    }

    @Test
    void unknownEmailGetsTheSameResponseWithoutSendingMail() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "missing-" + unique() + "@test.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If an account exists for this email, a reset code has been sent."));
        verify(emailService, never()).sendPasswordResetCode(anyString(), anyString(), anyString(), eq(10));
    }
}
