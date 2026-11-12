package com.erp.order;

import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void fullOrderLifecycle_createUpdateStatusCancelDeleteAndExports() throws Exception {
        Product p = new Product();
        p.setName("Laptop Test JUnit");
        p.setSku("TEST-SKU-" + System.nanoTime());
        p.setPrice(new BigDecimal("100000"));
        p.setStock(8);
        p = productRepository.saveAndFlush(p);

        String createPayload = """
                {
                  "customerName": "Công ty CP Giải pháp Doanh nghiệp An Phát",
                  "customerPhone": "0909 888 777",
                  "customerEmail": "muahang@anphat.vn",
                  "shippingAddress": "Tầng 8, Tòa nhà Lotte, Ba Đình, Hà Nội",
                  "note": "Giao giờ hành chính",
                  "paymentMethod": "BANK_TRANSFER",
                  "paymentStatus": "PAID",
                  "discountAmount": 0,
                  "taxRate": 8,
                  "items": [
                    { "productId": %d, "quantity": 2 }
                  ]
                }
                """.formatted(p.getId());

        MockMvc mvc = mockMvc();

        // 1. POST /api/orders -> 201 Created, stock 8 -> 6
        MvcResult createResult = mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderCode", startsWith("ORD-")))
                .andExpect(jsonPath("$.subtotal", is(200000.0)))
                .andExpect(jsonPath("$.taxAmount", is(16000.0)))
                .andExpect(jsonPath("$.totalAmount", is(216000.0)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long orderId = createdJson.get("id").asLong();
        assertEquals(6, productRepository.findById(p.getId()).orElseThrow().getStock());

        // 2. PUT /api/orders/{id} -> update quantity 2 -> 3 while PENDING, stock 6 -> 5
        String updatePayload = """
                {
                  "customerName": "Công ty An Phát Updated",
                  "paymentMethod": "BANK_TRANSFER",
                  "paymentStatus": "PAID",
                  "discountAmount": 10000,
                  "taxRate": 10,
                  "items": [
                    { "productId": %d, "quantity": 3 }
                  ]
                }
                """.formatted(p.getId());

        mvc.perform(put("/api/orders/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal", is(300000.0)))
                .andExpect(jsonPath("$.discountAmount", is(10000.0)))
                .andExpect(jsonPath("$.taxAmount", is(29000.0)))
                .andExpect(jsonPath("$.totalAmount", is(319000.0)));

        assertEquals(5, productRepository.findById(p.getId()).orElseThrow().getStock());

        // 3. PATCH status -> CONFIRMED -> CANCELLED restores stock 5 -> 8 and sets REFUNDED
        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\",\"note\":\"Khách đổi lịch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.paymentStatus", is("REFUNDED")));

        assertEquals(8, productRepository.findById(p.getId()).orElseThrow().getStock());

        // 4. POST /api/orders/inventory-adjust -> +12 => stock 20
        mvc.perform(post("/api/orders/inventory-adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + p.getId() + ",\"quantityDelta\":12,\"note\":\"Nhập bổ sung\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockBefore", is(8)))
                .andExpect(jsonPath("$.stockAfter", is(20)));

        // 5. GET endpoints & exports work cleanly
        mvc.perform(get("/api/orders")).andExpect(status().isOk());
        mvc.perform(get("/api/orders/" + orderId)).andExpect(status().isOk());
        mvc.perform(get("/api/orders/stats")).andExpect(status().isOk());
        mvc.perform(get("/api/orders/inventory-logs")).andExpect(status().isOk());

        // Inventory logs date-range filter (current month matches, 2020 returns 0)
        java.time.LocalDate today = java.time.LocalDate.now();
        String monthStart = today.withDayOfMonth(1).atStartOfDay().toString();
        String monthEnd = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59).toString();
        mvc.perform(get("/api/orders/inventory-logs")
                        .param("fromDate", monthStart)
                        .param("toDate", monthEnd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        mvc.perform(get("/api/orders/inventory-logs")
                        .param("fromDate", "2020-01-01T00:00:00")
                        .param("toDate", "2020-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
        mvc.perform(get("/api/orders/export-excel"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Don Hang ERP")));
        mvc.perform(get("/api/orders/export-csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ORD-")));

        // 6. DELETE cancelled order -> 200 OK
        mvc.perform(delete("/api/orders/" + orderId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createOrder_returns400WhenStockInsufficient() throws Exception {
        Product p = new Product();
        p.setName("Limited Item");
        p.setSku("LIM-SKU-" + System.nanoTime());
        p.setPrice(new BigDecimal("50000"));
        p.setStock(1);
        p = productRepository.saveAndFlush(p);

        String payload = """
                {
                  "customerName": "Khách Test",
                  "items": [ { "productId": %d, "quantity": 5 } ]
                }
                """.formatted(p.getId());

        mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("chỉ còn 1")));
    }
}
