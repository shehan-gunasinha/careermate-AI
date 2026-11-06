package com.careermate;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void registerReturnsTokenAndHashesPassword() throws Exception {
        String email = "reg-" + unique() + "@test.local";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Ann Lee", "email", email, "password", PASSWORD, "confirmPassword", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value("JOB_SEEKER"));
        String hash = userRepository.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash();
        assertFalse(hash.contains(PASSWORD));
        assertFalse(!hash.startsWith("$2"), "BCrypt hash expected");
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = "dup-" + unique() + "@test.local";
        registerUser(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Ann Lee", "email", email, "password", PASSWORD, "confirmPassword", PASSWORD))))
                .andExpect(status().isConflict());
    }

    @Test
    void mismatchedOrWeakPasswordsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "A B", "email", "x-" + unique() + "@test.local", "password", PASSWORD, "confirmPassword", "Different1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords do not match"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "A B", "email", "y-" + unique() + "@test.local", "password", "onlyletters", "confirmPassword", "onlyletters"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "email", "not-an-email", "password", "short", "confirmPassword", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    void loginSucceedsWithValidCredentialsAndFailsOtherwise() throws Exception {
        String email = "login-" + unique() + "@test.local";
        registerUser(email);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointsRequireValidJwt() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/jobs")).andExpect(status().isUnauthorized());
        String token = registerUser();
        mvc.perform(auth(get("/api/v1/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Test User"));
    }

    @Test
    void logoutRevokesToken() throws Exception {
        String token = registerUser();
        mvc.perform(auth(post("/api/v1/auth/logout"), token)).andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/auth/me"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        String email = "pw-" + unique() + "@test.local";
        String token = registerUser(email);
        mvc.perform(auth(put("/api/v1/auth/password"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "Nope1234", "newPassword", "NewPassw0rd", "confirmPassword", "NewPassw0rd"))))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(put("/api/v1/auth/password"), token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "NewPassw0rd", "confirmPassword", "NewPassw0rd"))))
                .andExpect(status().isOk());
        MvcResult ok = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", "NewPassw0rd")))).andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(200, ok.getResponse().getStatus());
    }

    @Test
    void adminEndpointsAreForbiddenForJobSeekers() throws Exception {
        String token = registerUser();
        mvc.perform(auth(post("/api/v1/admin/jobs/sync"), token)).andExpect(status().isForbidden());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("openapi")));
    }
}
