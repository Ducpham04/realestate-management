package com.realestate.integration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppointmentControllerIntegrationTest extends AbstractIntegrationTest {

    private static Map<String, Object> body(long propertyId, long customerId, Instant start) {
        Map<String, Object> body = new HashMap<>();
        body.put("propertyId", propertyId);
        body.put("customerId", customerId);
        body.put("scheduledAt", start.toString());
        body.put("durationMinutes", 60);
        body.put("note", "Integration viewing");
        return body;
    }

    @Test
    void scheduleRescheduleAndCancel() throws Exception {
        String token = agent1Token();
        long propertyId = createProperty(token, "Viewing flat", "SALE", 2_000_000_000L);
        long customerId = createCustomer(token, "Viewer");
        Instant start = Instant.now().plus(40, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        long id = data(mockMvc.perform(authJson(post("/api/appointments"), token, body(propertyId, customerId, start)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.agent.id").value(AGENT1_ID))
                .andExpect(jsonPath("$.data.property.id").value(propertyId))
                .andExpect(jsonPath("$.data.endAt").value(start.plus(1, ChronoUnit.HOURS).toString()))
                .andReturn()).path("id").asLong();

        Instant moved = start.plus(3, ChronoUnit.HOURS);
        mockMvc.perform(authJson(put("/api/appointments/{id}", id), token, body(propertyId, customerId, moved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduledAt").value(moved.toString()));

        mockMvc.perform(authJson(patch("/api/appointments/{id}/status", id), token,
                        Map.of("status", "CANCELLED", "note", "Customer travelling")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.note").value("Customer travelling"));

        mockMvc.perform(authJson(patch("/api/appointments/{id}/status", id), token, Map.of("status", "COMPLETED")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void overlappingSlotForSameAgentIsConflict() throws Exception {
        String token = agent1Token();
        long propertyId = createProperty(token, "Busy flat", "SALE", 2_000_000_000L);
        long customerId = createCustomer(token, "Busy viewer");
        Instant start = Instant.now().plus(50, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        mockMvc.perform(authJson(post("/api/appointments"), token, body(propertyId, customerId, start)))
                .andExpect(status().isCreated());
        mockMvc.perform(authJson(post("/api/appointments"), token,
                        body(propertyId, customerId, start.plus(30, ChronoUnit.MINUTES))))
                .andExpect(status().isConflict());
        // Back-to-back slots are fine.
        mockMvc.perform(authJson(post("/api/appointments"), token,
                        body(propertyId, customerId, start.plus(60, ChronoUnit.MINUTES))))
                .andExpect(status().isCreated());
    }

    @Test
    void pastTimeAndSoldPropertyAreRejected() throws Exception {
        String token = agent1Token();
        long customerId = createCustomer(token, "Late viewer");

        mockMvc.perform(authJson(post("/api/appointments"), token,
                        body(2, customerId, Instant.now().minus(1, ChronoUnit.DAYS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("scheduledAt"));

        // Seed property 1 is SOLD.
        mockMvc.perform(authJson(post("/api/appointments"), token,
                        body(1, customerId, Instant.now().plus(60, ChronoUnit.DAYS))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void agentCannotBookForAnotherAgentsCustomer() throws Exception {
        long foreignCustomer = createCustomer(agent2Token(), "Agent2 customer");
        long propertyId = createProperty(agent1Token(), "Any flat", "RENT", 10_000_000L);

        mockMvc.perform(authJson(post("/api/appointments"), agent1Token(),
                        body(propertyId, foreignCustomer, Instant.now().plus(70, ChronoUnit.DAYS))))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchIsScopedAndFiltersByDateRange() throws Exception {
        Instant from = Instant.now();
        Instant to = from.plus(10, ChronoUnit.DAYS);
        mockMvc.perform(auth(get("/api/appointments"), agent1Token())
                        .param("status", "SCHEDULED")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].agent.id", everyItem(is((int) AGENT1_ID))))
                .andExpect(jsonPath("$.data.content[*].status", everyItem(is("SCHEDULED"))));

        mockMvc.perform(auth(get("/api/appointments"), adminToken())
                        .param("from", to.toString())
                        .param("to", from.toString()))
                .andExpect(status().isUnprocessableEntity());
    }
}
