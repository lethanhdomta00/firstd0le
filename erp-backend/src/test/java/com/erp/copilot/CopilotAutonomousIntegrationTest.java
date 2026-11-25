package com.erp.copilot;

import com.erp.copilot.dto.ActionChainDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class CopilotAutonomousIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("1. Bản tin điểm báo điều hành buổi sáng (Morning Executive Audio Briefing)")
    void testMorningBriefingApi() throws Exception {
        MvcResult result = mockMvc().perform(get("/api/copilot/morning-briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.briefingDate", notNullValue()))
                .andExpect(jsonPath("$.yesterdayRevenue", notNullValue()))
                .andExpect(jsonPath("$.yesterdayGrossProfit", notNullValue()))
                .andExpect(jsonPath("$.dueDebtAmount", notNullValue()))
                .andExpect(jsonPath("$.pendingPoCount", notNullValue()))
                .andExpect(jsonPath("$.audioScriptText", containsString("Chào sếp!")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(root.get("audioScriptText").asText());
        assertTrue(root.get("priorityActions").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("2. Quét & bóc tách hóa đơn chứng từ thông minh qua file upload (Smart OCR Vision)")
    void testSmartOcrScanApi() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hoa_don_vat_tan_binh.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content bytes".getBytes()
        );

        MvcResult result = mockMvc().perform(multipart("/api/copilot/ocr-scan")
                        .file(file)
                        .param("typeHint", "PURCHASE_ORDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber", notNullValue()))
                .andExpect(jsonPath("$.supplierName", containsString("Tân Bình")))
                .andExpect(jsonPath("$.taxCode", is("0314567890")))
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalAmount", notNullValue()))
                .andExpect(jsonPath("$.confidenceScore", greaterThanOrEqualTo(0.95)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("PURCHASE_ORDER", root.get("suggestedDocumentType").asText());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("3. Quét & bóc tách hóa đơn qua Base64 (Smart OCR Vision Base64)")
    void testSmartOcrScanBase64Api() throws Exception {
        Map<String, String> payload = Map.of(
                "base64", "data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=",
                "filename", "invoice_bill.png",
                "typeHint", "PAYMENT_VOUCHER"
        );

        mockMvc().perform(post("/api/copilot/ocr-scan-base64")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber", notNullValue()))
                .andExpect(jsonPath("$.taxCode", is("0314567890")))
                .andExpect(jsonPath("$.suggestedDocumentType", is("PAYMENT_VOUCHER")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("4. Động cơ phát hiện bất thường & rủi ro gian lận (AI Anomaly & Fraud Detection)")
    void testAnomalyDetectionApi() throws Exception {
        mockMvc().perform(get("/api/copilot/anomaly-detection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlerts", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.criticalCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.alerts", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.alerts[?(@.category == 'ABNORMAL_DISCOUNT')]", notNullValue()))
                .andExpect(jsonPath("$.alerts[?(@.category == 'CASH_REVENUE_MISMATCH')]", notNullValue()))
                .andExpect(jsonPath("$.alerts[?(@.category == 'BURN_RATE_SPIKE')]", notNullValue()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("5. Chuỗi tác vụ tự động đa phân hệ (Cross-Module Action Chaining): Parse và Thực thi 1-Click")
    void testActionChainingParseAndExecute() throws Exception {
        String prompt = "Soạn đơn mua 50 cái áo Polo từ Xưởng Tân Bình, lấy tiền từ tài khoản Vietcombank đặt cọc trước 30%, và gửi thông báo qua Telegram cho thủ kho chuẩn bị chỗ chứa hàng!";

        // 1. Phân rã câu lệnh thành ActionChain
        MvcResult parseResult = mockMvc().perform(post("/api/copilot/chain/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("prompt", prompt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chainId", notNullValue()))
                .andExpect(jsonPath("$.steps", hasSize(3)))
                .andExpect(jsonPath("$.executed", is(false)))
                .andReturn();

        ActionChainDto chain = objectMapper.readValue(parseResult.getResponse().getContentAsString(), ActionChainDto.class);
        assertNotNull(chain.getChainId());
        assertEquals(3, chain.getSteps().size());

        // 2. Thực thi chuỗi tác vụ liên hoàn 1-Click
        MvcResult execResult = mockMvc().perform(post("/api/copilot/chain/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chain)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed", is(true)))
                .andExpect(jsonPath("$.executionResult", containsString("thành công")))
                .andExpect(jsonPath("$.steps[0].status", is("SUCCESS")))
                .andExpect(jsonPath("$.steps[1].status", is("SUCCESS")))
                .andExpect(jsonPath("$.steps[2].status", is("SUCCESS")))
                .andReturn();

        JsonNode root = objectMapper.readTree(execResult.getResponse().getContentAsString());
        assertTrue(root.get("executed").asBoolean());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("6. Kích hoạt cảnh báo tự hành chủ động (Autonomous Proactive Alert)")
    void testTriggerProactiveAlertApi() throws Exception {
        mockMvc().perform(post("/api/copilot/trigger-proactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
