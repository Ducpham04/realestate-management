package com.realestate.integration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertyControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void agentCreatesPropertyAssignedToThemselves() throws Exception {
        var body = propertyBody("Agent created flat", "SALE", 3_000_000_000L);
        body.put("agentId", AGENT2_ID);

        mockMvc.perform(authJson(post("/api/properties"), agent1Token(), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.agent.id").value(AGENT1_ID))
                .andExpect(jsonPath("$.data.address.city").value("Test City"))
                .andExpect(jsonPath("$.data.images[0]").value("https://example.com/a.jpg"));
    }

    @Test
    void validationErrorsAreReportedPerField() throws Exception {
        var body = propertyBody("", "SALE", -5);
        body.put("images", java.util.List.of("ftp://bad"));

        mockMvc.perform(authJson(post("/api/properties"), agent1Token(), body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("images[0]")));
    }

    @Test
    void searchSupportsFiltersPaginationAndSorting() throws Exception {
        String token = adminToken();
        mockMvc.perform(auth(get("/api/properties"), token)
                        .param("city", "Hồ Chí Minh")
                        .param("listingType", "SALE")
                        .param("minPrice", "5000000000")
                        .param("maxPrice", "40000000000")
                        .param("sort", "price,desc")
                        .param("size", "3")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[*].address.city", everyItem(is("Hồ Chí Minh"))))
                .andExpect(jsonPath("$.data.content[*].listingType", everyItem(is("SALE"))))
                .andExpect(jsonPath("$.data.content[*].price", everyItem(greaterThanOrEqualTo(5_000_000_000.0))))
                .andExpect(jsonPath("$.data.content[*].price", everyItem(lessThanOrEqualTo(40_000_000_000.0))))
                .andExpect(jsonPath("$.data.content[0].price").value(32_000_000_000.0));

        mockMvc.perform(auth(get("/api/properties"), token).param("keyword", "vinhomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void invalidSortAndParametersReturnBadRequest() throws Exception {
        String token = agent1Token();
        mockMvc.perform(auth(get("/api/properties"), token).param("sort", "agent.password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        mockMvc.perform(auth(get("/api/properties"), token).param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerCanUpdateButOtherAgentIsForbidden() throws Exception {
        String agent1 = agent1Token();
        long id = createProperty(agent1, "Owned by agent1", "RENT", 20_000_000L);

        var update = propertyBody("Renamed by owner", "RENT", 22_000_000L);
        mockMvc.perform(authJson(put("/api/properties/{id}", id), agent1, update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed by owner"))
                .andExpect(jsonPath("$.data.price").value(22_000_000.0));

        mockMvc.perform(authJson(put("/api/properties/{id}", id), agent2Token(), update))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(authJson(patch("/api/properties/{id}/status", id), agent1, Map.of("status", "INACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    void onlyAdminCanDelete() throws Exception {
        long id = createProperty(agent1Token(), "To be deleted", "SALE", 1_000_000_000L);

        mockMvc.perform(auth(delete("/api/properties/{id}", id), agent1Token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(delete("/api/properties/{id}", id), adminToken()))
                .andExpect(status().isOk());
        mockMvc.perform(auth(get("/api/properties/{id}", id), adminToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void propertyWithHistoryCannotBeDeleted() throws Exception {
        // Seed property 1 has a closed deal.
        mockMvc.perform(auth(delete("/api/properties/1"), adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    void getReturnsFullDetails() throws Exception {
        JsonNode property = data(mockMvc.perform(auth(get("/api/properties/4"), agent2Token()))
                .andExpect(status().isOk())
                .andReturn());
        org.assertj.core.api.Assertions.assertThat(property.path("images").size()).isGreaterThan(0);
        org.assertj.core.api.Assertions.assertThat(property.path("agent").path("fullName").asText()).isNotBlank();
    }
}
