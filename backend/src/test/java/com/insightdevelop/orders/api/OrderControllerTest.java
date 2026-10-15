package com.insightdevelop.orders.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through HTTP against the seeded sample data. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerTest {

    private static final String UI_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mvc;

    @Test
    void listsOrdersWithEligibility() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)))
                .andExpect(jsonPath("$[0].orderId").value("ORD-1001"))
                .andExpect(jsonPath("$[0].eligibility.eligible").value(true))
                .andExpect(jsonPath("$[0].eligibility.score").value(100))
                .andExpect(jsonPath("$[0].eligibility.decision").value("ELIGIBLE"))
                .andExpect(jsonPath("$[0].canApproveExpedited").value(true));
    }

    @Test
    void eligibilityEndpointReturnsContract() throws Exception {
        mvc.perform(get("/api/orders/ORD-1007/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.score").value(40))
                .andExpect(jsonPath("$.reasons", hasSize(4)))
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.decision").value("NOT_ELIGIBLE"));
    }

    @Test
    void approvalSucceedsThenIsIdempotent() throws Exception {
        mvc.perform(post("/api/orders/ORD-1001/approve-expedited"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.alreadyApproved").value(false))
                .andExpect(jsonPath("$.eligibility.eligible").value(true));

        mvc.perform(post("/api/orders/ORD-1001/approve-expedited"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.alreadyApproved").value(true));

        mvc.perform(get("/api/orders/ORD-1001"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.canApproveExpedited").value(false));
    }

    @Test
    void ineligibleOrderReturnsStructuredError() throws Exception {
        mvc.perform(post("/api/orders/ORD-1003/approve-expedited"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.orderId").value("ORD-1003"))
                .andExpect(jsonPath("$.reasons", hasItem("Inventory is not available")))
                .andExpect(jsonPath("$.eligibility.eligible").value(false));
    }

    @Test
    void rejectedOrderReturnsConflict() throws Exception {
        mvc.perform(post("/api/orders/ORD-1011/approve-expedited"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        mvc.perform(post("/api/orders/NOPE/approve-expedited"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void refreshInventoryCanFlipAvailability() throws Exception {
        mvc.perform(post("/api/orders/ORD-1003/inventory/refresh")
                        .contentType("application/json").content("{\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibility.eligible").value(true));
    }

    @Test
    void corsPreflightAllowsTheFrontendOrigin() throws Exception {
        mvc.perform(options("/api/orders/ORD-1001/approve-expedited")
                        .header(HttpHeaders.ORIGIN, UI_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, UI_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")));

        mvc.perform(get("/api/orders").header(HttpHeaders.ORIGIN, UI_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, UI_ORIGIN));
    }

    @Test
    void publishesOpenApiSpecAndSwaggerUi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Expedited Orders API"))
                .andExpect(jsonPath("$.paths['/api/orders/{id}/approve-expedited'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}/approve-expedited'].post.responses['409']").exists());

        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void corsRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/api/orders")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
