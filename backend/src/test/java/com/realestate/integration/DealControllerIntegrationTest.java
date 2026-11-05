package com.realestate.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DealControllerIntegrationTest extends AbstractIntegrationTest {

    private static Map<String, Object> body(long propertyId, long customerId, long amount, long commission) {
        return Map.of("propertyId", propertyId, "customerId", customerId, "amount", amount,
                "commission", commission, "notes", "integration");
    }

    private String propertyStatus(String token, long id) throws Exception {
        return data(mockMvc.perform(auth(get("/api/properties/{id}", id), token)).andReturn())
                .path("status").asText();
    }

    @Test
    void closingADealSellsThePropertyAndCountsTowardsRevenue() throws Exception {
        String token = agent1Token();
        long propertyId = createProperty(token, "Deal flat", "SALE", 4_000_000_000L);
        long customerId = createCustomer(token, "Buyer");

        JsonNode before = data(mockMvc.perform(auth(get("/api/dashboard/summary"), token)).andReturn());

        long dealId = data(mockMvc.perform(authJson(post("/api/deals"), token,
                        body(propertyId, customerId, 3_900_000_000L, 78_000_000L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("NEGOTIATING"))
                .andExpect(jsonPath("$.data.property.id").value(propertyId))
                .andReturn()).path("id").asLong();
        assertThat(propertyStatus(token, propertyId)).isEqualTo("RESERVED");

        // A second deal on a reserved property is rejected.
        mockMvc.perform(authJson(post("/api/deals"), token, body(propertyId, customerId, 1, 0)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(authJson(patch("/api/deals/{id}/status", dealId), token, Map.of("status", "DEPOSITED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DEPOSITED"));
        mockMvc.perform(authJson(patch("/api/deals/{id}/status", dealId), token, Map.of("status", "CLOSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
        assertThat(propertyStatus(token, propertyId)).isEqualTo("SOLD");

        JsonNode after = data(mockMvc.perform(auth(get("/api/dashboard/summary"), token)).andReturn());
        assertThat(after.path("closedDealsThisMonth").asLong())
                .isEqualTo(before.path("closedDealsThisMonth").asLong() + 1);
        assertThat(after.path("revenueThisMonth").decimalValue()
                .subtract(before.path("revenueThisMonth").decimalValue()))
                .isEqualByComparingTo(new BigDecimal("3900000000"));

        // Closed deals are final and cannot be deleted.
        mockMvc.perform(authJson(patch("/api/deals/{id}/status", dealId), token, Map.of("status", "CANCELLED")))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(auth(delete("/api/deals/{id}", dealId), adminToken()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancellingADealReleasesTheProperty() throws Exception {
        String token = agent1Token();
        long propertyId = createProperty(token, "Cancel flat", "RENT", 15_000_000L);
        long customerId = createCustomer(token, "Tenant");

        long dealId = data(mockMvc.perform(authJson(post("/api/deals"), token,
                        body(propertyId, customerId, 180_000_000L, 15_000_000L)))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asLong();

        mockMvc.perform(authJson(patch("/api/deals/{id}/status", dealId), token, Map.of("status", "CANCELLED")))
                .andExpect(status().isOk());
        assertThat(propertyStatus(token, propertyId)).isEqualTo("AVAILABLE");
    }

    @Test
    void commissionGreaterThanAmountIsInvalid() throws Exception {
        String token = agent1Token();
        mockMvc.perform(authJson(post("/api/deals"), token, body(2, 1, 100, 200)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void agentsCannotDeleteDealsAndOnlySeeTheirOwn() throws Exception {
        String agent1 = agent1Token();
        long propertyId = createProperty(agent1, "Scoped flat", "SALE", 1_500_000_000L);
        long customerId = createCustomer(agent1, "Scoped buyer");
        long dealId = data(mockMvc.perform(authJson(post("/api/deals"), agent1,
                        body(propertyId, customerId, 1_400_000_000L, 28_000_000L)))
                .andReturn()).path("id").asLong();

        mockMvc.perform(auth(delete("/api/deals/{id}", dealId), agent1)).andExpect(status().isForbidden());
        mockMvc.perform(auth(get("/api/deals/{id}", dealId), agent2Token())).andExpect(status().isNotFound());

        mockMvc.perform(auth(delete("/api/deals/{id}", dealId), adminToken())).andExpect(status().isOk());
        assertThat(propertyStatus(agent1, propertyId)).isEqualTo("AVAILABLE");
    }

    @Test
    void searchFiltersByStatus() throws Exception {
        mockMvc.perform(auth(get("/api/deals"), adminToken()).param("status", "CLOSED").param("sort", "amount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("CLOSED"))
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }
}
