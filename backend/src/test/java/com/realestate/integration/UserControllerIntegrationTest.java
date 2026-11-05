package com.realestate.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminCreatesAgentWhoCanLogIn() throws Exception {
        String email = "new.agent." + System.nanoTime() + "@realestate.com";
        long id = data(mockMvc.perform(authJson(post("/api/users"), adminToken(), Map.of(
                        "fullName", "New Agent", "email", email, "password", "Secret123", "role", "AGENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("AGENT"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andReturn()).path("id").asLong();

        login(email, "Secret123");

        // Disabled accounts can no longer log in.
        mockMvc.perform(authJson(put("/api/users/{id}", id), adminToken(), Map.of(
                        "fullName", "New Agent", "role", "AGENT", "enabled", false)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Secret123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DISABLED"));
    }

    @Test
    void duplicateEmailAndWeakPasswordAreRejected() throws Exception {
        String token = adminToken();
        mockMvc.perform(authJson(post("/api/users"), token, Map.of(
                        "fullName", "Dup", "email", AGENT1_EMAIL, "password", "Secret123", "role", "AGENT")))
                .andExpect(status().isConflict());
        mockMvc.perform(authJson(post("/api/users"), token, Map.of(
                        "fullName", "Weak", "email", "weak@realestate.com", "password", "short", "role", "AGENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void agentsCannotManageUsers() throws Exception {
        String token = agent1Token();
        mockMvc.perform(auth(get("/api/users"), token)).andExpect(status().isForbidden());
        mockMvc.perform(authJson(post("/api/users"), token, Map.of(
                        "fullName", "X", "email", "x@realestate.com", "password", "Secret123", "role", "ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/users/agents"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void adminSearchesUsersByRole() throws Exception {
        mockMvc.perform(auth(get("/api/users"), adminToken()).param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].role").value("ADMIN"));
    }
}
