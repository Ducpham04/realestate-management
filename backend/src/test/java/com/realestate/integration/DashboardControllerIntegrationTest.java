package com.realestate.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DashboardControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void propertiesByStatusSumsToTotal() throws Exception {
        String token = adminToken();
        JsonNode byStatus = data(mockMvc.perform(auth(get("/api/dashboard/properties-by-status"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andReturn());
        JsonNode summary = data(mockMvc.perform(auth(get("/api/dashboard/summary"), token))
                .andExpect(status().isOk())
                .andReturn());

        long sum = 0;
        for (JsonNode node : byStatus) {
            sum += node.path("count").asLong();
        }
        assertThat(summary.path("totalProperties").asLong()).isEqualTo(sum);
    }

    @Test
    void revenueByMonthIsZeroFilledAndEndsWithCurrentMonth() throws Exception {
        JsonNode months = data(mockMvc.perform(auth(get("/api/dashboard/revenue-by-month"), adminToken())
                        .param("months", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(12))
                .andReturn());

        assertThat(months.get(11).path("month").asText()).isEqualTo(YearMonth.now(ZoneOffset.UTC).toString());
        long totalDeals = 0;
        for (JsonNode m : months) {
            totalDeals += m.path("deals").asLong();
        }
        // The seed data contains closed deals spread across the past year.
        assertThat(totalDeals).isGreaterThanOrEqualTo(10);
    }

    @Test
    void agentRevenueIsScopedToOwnDeals() throws Exception {
        JsonNode admin = data(mockMvc.perform(auth(get("/api/dashboard/revenue-by-month"), adminToken())).andReturn());
        JsonNode agent = data(mockMvc.perform(auth(get("/api/dashboard/revenue-by-month"), agent2Token())).andReturn());

        double adminTotal = 0;
        double agentTotal = 0;
        for (int i = 0; i < admin.size(); i++) {
            adminTotal += admin.get(i).path("revenue").asDouble();
            agentTotal += agent.get(i).path("revenue").asDouble();
        }
        assertThat(agentTotal).isPositive().isLessThan(adminTotal);
    }

    @Test
    void monthsParameterIsValidated() throws Exception {
        mockMvc.perform(auth(get("/api/dashboard/revenue-by-month"), adminToken()).param("months", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
