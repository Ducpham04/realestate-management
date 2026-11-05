package com.realestate.integration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomerControllerIntegrationTest extends AbstractIntegrationTest {

    private Map<String, Object> body(String name, String phone) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", name);
        body.put("email", "customer@example.com");
        body.put("phone", phone);
        body.put("type", "TENANT");
        body.put("budgetMin", 10_000_000);
        body.put("budgetMax", 20_000_000);
        body.put("preferredLocation", "Quận 1");
        return body;
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        String token = agent1Token();
        String phone = uniquePhone();

        long id = data(mockMvc.perform(authJson(post("/api/customers"), token, body("Khách Mới", phone)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.agent.id").value(AGENT1_ID))
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andReturn()).path("id").asLong();

        mockMvc.perform(authJson(put("/api/customers/{id}", id), token, body("Khách Đã Sửa", phone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Khách Đã Sửa"));

        mockMvc.perform(auth(get("/api/customers/{id}", id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredLocation").value("Quận 1"));

        mockMvc.perform(auth(delete("/api/customers/{id}", id), token))
                .andExpect(status().isOk());
        mockMvc.perform(auth(get("/api/customers/{id}", id), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicatePhoneIsConflict() throws Exception {
        String token = agent1Token();
        String phone = uniquePhone();
        mockMvc.perform(authJson(post("/api/customers"), token, body("First", phone)))
                .andExpect(status().isCreated());

        mockMvc.perform(authJson(post("/api/customers"), agent2Token(), body("Second", phone)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void invalidBudgetRangeIsRejected() throws Exception {
        var body = body("Budget", uniquePhone());
        body.put("budgetMin", 50);
        body.put("budgetMax", 10);

        mockMvc.perform(authJson(post("/api/customers"), agent1Token(), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("budgetRangeValid"));
    }

    @Test
    void agentsOnlySeeTheirOwnCustomers() throws Exception {
        mockMvc.perform(auth(get("/api/customers"), agent1Token()).param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].agent.id", everyItem(is((int) AGENT1_ID))));

        // Seed customer 6 belongs to agent2: agent1 must not learn it exists.
        mockMvc.perform(auth(get("/api/customers/6"), agent1Token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth(get("/api/customers/6"), adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void keywordSearchMatchesNameEmailOrPhone() throws Exception {
        mockMvc.perform(auth(get("/api/customers"), adminToken()).param("keyword", "0912345007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Đặng Minh Khôi"));
    }

    @Test
    void customerWithHistoryCannotBeDeleted() throws Exception {
        mockMvc.perform(auth(delete("/api/customers/1"), adminToken()))
                .andExpect(status().isConflict());
    }
}
