package com.realestate.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;

/**
 * Boots the full application against a real MySQL 8 started by Testcontainers. The container is a
 * JVM-wide singleton shared by every integration test class, and Flyway applies the real migrations
 * (including demo seed data) exactly as in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@realestate.com";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String AGENT1_EMAIL = "agent1@realestate.com";
    protected static final String AGENT2_EMAIL = "agent2@realestate.com";
    protected static final String AGENT_PASSWORD = "Agent@123";
    protected static final long AGENT1_ID = 2L;
    protected static final long AGENT2_ID = 3L;

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @ServiceConnection
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("realestate")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    static {
        MYSQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD).path("accessToken").asText();
    }

    protected String agent1Token() throws Exception {
        return login(AGENT1_EMAIL, AGENT_PASSWORD).path("accessToken").asText();
    }

    protected String agent2Token() throws Exception {
        return login(AGENT2_EMAIL, AGENT_PASSWORD).path("accessToken").asText();
    }

    /** Logs in and returns the {@code data} node of the response. */
    protected JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return data(result);
    }

    protected MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder authJson(MockHttpServletRequestBuilder builder, String token,
                                                     Object body) throws Exception {
        return auth(builder, token).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    /** Unique, valid Vietnamese-style phone number so tests never collide on the unique constraint. */
    protected static String uniquePhone() {
        return "07%08d".formatted(SEQUENCE.incrementAndGet());
    }

    protected static Map<String, Object> propertyBody(String title, String listingType, long price) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", title);
        body.put("description", "Integration test property");
        body.put("type", "APARTMENT");
        body.put("listingType", listingType);
        body.put("price", price);
        body.put("area", 70);
        body.put("bedrooms", 2);
        body.put("bathrooms", 1);
        body.put("address", Map.of("street", "1 Test Street", "ward", "Ward 1", "district", "Test District",
                "city", "Test City"));
        body.put("images", java.util.List.of("https://example.com/a.jpg"));
        return body;
    }

    protected long createProperty(String token, String title, String listingType, long price) throws Exception {
        MvcResult result = mockMvc.perform(authJson(post("/api/properties"), token,
                        propertyBody(title, listingType, price)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }

    protected long createCustomer(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(authJson(post("/api/customers"), token, Map.of(
                        "fullName", name,
                        "phone", uniquePhone(),
                        "type", "BUYER")))
                .andExpect(status().isCreated())
                .andReturn();
        return data(result).path("id").asLong();
    }
}
