package com.erp.order;

import com.erp.model.Product;
import com.erp.order.model.Order;
import com.erp.order.model.PaymentMethod;
import com.erp.order.model.PaymentStatus;
import com.erp.order.repository.OrderRepository;
import com.erp.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class VietQrIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private Long createTestOrderId() throws Exception {
        Product p = new Product();
        p.setName("Laptop Test VietQR");
        p.setSku("TEST-VQR-" + System.nanoTime());
        p.setPrice(new BigDecimal("150000"));
        p.setStock(10);
        p = productRepository.saveAndFlush(p);

        String createPayload = """
                {
                  "customerName": "Trần Văn Khách",
                  "customerPhone": "0912345678",
                  "customerEmail": "khach@test.com",
                  "paymentMethod": "BANK_TRANSFER",
                  "paymentStatus": "UNPAID",
                  "items": [
                    { "productId": %d, "quantity": 1 }
                  ]
                }
                """.formatted(p.getId());

        MvcResult res = mockMvc().perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = OBJECT_MAPPER.readTree(res.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("1. Sinh mã VietQR động chuẩn NAPAS chứa thông tin chuyển khoản và URL ảnh QR")
    void testGenerateVietQr() throws Exception {
        Long orderId = createTestOrderId();
        Order order = orderRepository.findById(orderId).orElseThrow();

        mockMvc().perform(get("/api/orders/" + orderId + "/vietqr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.orderCode").value(order.getOrderCode()))
                .andExpect(jsonPath("$.bankId").value("MB"))
                .andExpect(jsonPath("$.paid").value(false))
                .andExpect(jsonPath("$.qrImageUrl").isNotEmpty())
                .andExpect(jsonPath("$.transferContent").value("DH " + order.getOrderCode()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("2. Xác nhận thanh toán VietQR giả lập (Mock Payment) cập nhật trạng thái PAID")
    void testPayVietQrMock() throws Exception {
        Long orderId = createTestOrderId();

        mockMvc().perform(post("/api/orders/" + orderId + "/pay-vietqr-mock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        Order updated = orderRepository.findById(orderId).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(updated.getPaymentMethod()).isEqualTo(PaymentMethod.VIETQR);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("3. Webhook thanh toán tự động (Không cần Bearer token) nhận diện mã đơn và cập nhật PAID")
    void testPaymentWebhook() throws Exception {
        Long orderId = createTestOrderId();
        Order order = orderRepository.findById(orderId).orElseThrow();

        String webhookPayload = """
                {
                  "gateway": "NAPAS_247",
                  "transactionId": "TXN-NAPAS-888999",
                  "content": "Thanh toan tien hang %s tai Ha Noi",
                  "amount": 150000
                }
                """.formatted(order.getOrderCode());

        mockMvc().perform(post("/api/orders/payment-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderCode").value(order.getOrderCode()))
                .andExpect(jsonPath("$.status").value("PAID"));

        Order updated = orderRepository.findById(orderId).orElseThrow();
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
