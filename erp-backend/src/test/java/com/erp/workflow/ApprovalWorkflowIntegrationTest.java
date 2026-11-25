package com.erp.workflow;

import com.erp.workflow.dto.ApprovalDecisionRequest;
import com.erp.workflow.dto.ApprovalMatrixRuleDto;
import com.erp.workflow.dto.SubmitApprovalRequest;
import com.erp.workflow.model.*;
import com.erp.workflow.repository.ApprovalMatrixRuleRepository;
import com.erp.workflow.repository.ApprovalRequestRepository;
import com.erp.workflow.repository.ImmutableAuditLogRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class ApprovalWorkflowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApprovalMatrixRuleRepository ruleRepository;

    @Autowired
    private ApprovalRequestRepository requestRepository;

    @Autowired
    private ImmutableAuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("1. Tự động phê duyệt ngay (Auto-Approve) khi giá trị PO dưới 20 triệu theo quy tắc ma trận")
    void testAutoApproveWhenAmountUnderThreshold() throws Exception {
        SubmitApprovalRequest submitReq = SubmitApprovalRequest.builder()
                .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                .documentId(991L)
                .documentCode("PO-TEST-001")
                .title("Đơn mua hàng văn phòng phẩm 12.000.000đ")
                .amount(new BigDecimal("12000000.00"))
                .currency("VND")
                .build();

        MvcResult result = mockMvc().perform(post("/api/workflow/approvals/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.currentLevel", is(1)))
                .andExpect(jsonPath("$.totalLevels", is(1)))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals("APPROVED", root.get("status").asText());
        assertTrue(root.get("decisionReason").asText().contains("Tự động"));
    }

    @Test
    @WithMockUser(username = "nhanvien", roles = {"SALES", "USER"})
    @DisplayName("2. Khởi tạo yêu cầu duyệt PO 50 triệu cần 2 cấp duyệt (Trưởng phòng -> Kế toán trưởng)")
    void testMultiLevelApprovalWorkflowStep1Submit() throws Exception {
        SubmitApprovalRequest submitReq = SubmitApprovalRequest.builder()
                .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                .documentId(992L)
                .documentCode("PO-TEST-002")
                .title("Đơn mua thiết bị điện tử 50.000.000đ")
                .amount(new BigDecimal("50000000.00"))
                .currency("VND")
                .build();

        MvcResult result = mockMvc().perform(post("/api/workflow/approvals/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.currentLevel", is(1)))
                .andExpect(jsonPath("$.totalLevels", is(2)))
                .andExpect(jsonPath("$.currentApproverRole", is("ROLE_MANAGER")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long requestId = root.get("id").asLong();
        assertNotNull(requestId);
    }

    @Test
    @DisplayName("3. Quy trình duyệt đa cấp đầy đủ: Cấp 1 duyệt -> Cấp 2 duyệt -> Hoàn tất APPROVED")
    void testFullMultiLevelApprovalFlow() throws Exception {
        // Bước 1: Submit yêu cầu PO 50 triệu
        ApprovalRequest apr = ApprovalRequest.builder()
                .requestCode("APR-TEST-MULTI-01")
                .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                .documentId(993L)
                .documentCode("PO-TEST-003")
                .title("Đơn PO 50 triệu duyệt 2 cấp")
                .amount(new BigDecimal("50000000.00"))
                .currency("VND")
                .currentLevel(1)
                .totalLevels(2)
                .status(ApprovalStatus.PENDING)
                .currentApproverRole("ROLE_MANAGER")
                .requesterUsername("nhanvien")
                .requesterFullName("Nguyễn Văn A")
                .build();
        ApprovalRequest saved = requestRepository.save(apr);

        // Bước 2: Cấp 1 (ROLE_MANAGER) duyệt
        ApprovalDecisionRequest decisionLevel1 = ApprovalDecisionRequest.builder()
                .action("APPROVE")
                .comment("Trưởng phòng kiểm tra kế hoạch mua sắm hợp lệ")
                .build();

        MockMvc managerMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        managerMvc.perform(post("/api/workflow/approvals/" + saved.getId() + "/decision")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("manager1").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionLevel1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.currentLevel", is(2)))
                .andExpect(jsonPath("$.currentApproverRole", is("ROLE_ACCOUNTANT")));

        // Bước 3: Cấp 2 (ROLE_ACCOUNTANT) duyệt hoàn tất
        ApprovalDecisionRequest decisionLevel2 = ApprovalDecisionRequest.builder()
                .action("APPROVE")
                .comment("Kế toán trưởng xác nhận đã dự trù ngân sách thanh toán")
                .build();

        MockMvc accountantMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        accountantMvc.perform(post("/api/workflow/approvals/" + saved.getId() + "/decision")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("accountant1").roles("ACCOUNTANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(decisionLevel2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.currentLevel", is(2)));

        // Xác nhận trạng thái trong database
        ApprovalRequest reloaded = requestRepository.findById(saved.getId()).orElseThrow();
        assertEquals(ApprovalStatus.APPROVED, reloaded.getStatus());
        assertEquals(2, reloaded.getStepHistories().size());
    }

    @Test
    @DisplayName("4. Từ chối phê duyệt kèm lý do (REJECT) -> Cập nhật trạng thái chứng từ")
    void testRejectApprovalWithReason() throws Exception {
        ApprovalRequest apr = ApprovalRequest.builder()
                .requestCode("APR-TEST-REJECT-01")
                .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                .documentId(994L)
                .documentCode("PO-TEST-004")
                .title("Đơn PO kiểm tra từ chối")
                .amount(new BigDecimal("35000000.00"))
                .currency("VND")
                .currentLevel(1)
                .totalLevels(2)
                .status(ApprovalStatus.PENDING)
                .currentApproverRole("ROLE_MANAGER")
                .requesterUsername("nhanvien")
                .build();
        ApprovalRequest saved = requestRepository.save(apr);

        ApprovalDecisionRequest rejectDecision = ApprovalDecisionRequest.builder()
                .action("REJECT")
                .comment("Tạm dừng mua sắm do vượt ngân sách quý 4")
                .build();

        mockMvc().perform(post("/api/workflow/approvals/" + saved.getId() + "/decision")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("manager1").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDecision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.decisionReason", containsString("vượt ngân sách")));

        ApprovalRequest reloaded = requestRepository.findById(saved.getId()).orElseThrow();
        assertEquals(ApprovalStatus.REJECTED, reloaded.getStatus());
    }

    @Test
    @DisplayName("5. Phê duyệt nhanh 1-Click qua One-time Token (Public URL không cần login)")
    void testQuickTokenDecision() throws Exception {
        String token = "TEST-TOKEN-QUICK-001";
        ApprovalRequest apr = ApprovalRequest.builder()
                .requestCode("APR-TEST-TOKEN-01")
                .documentType(ApprovalDocumentType.PURCHASE_ORDER)
                .documentId(995L)
                .documentCode("PO-TEST-005")
                .title("Đơn PO kiểm tra duyệt qua email token")
                .amount(new BigDecimal("15000000.00"))
                .currency("VND")
                .currentLevel(1)
                .totalLevels(1)
                .status(ApprovalStatus.PENDING)
                .currentApproverRole("ROLE_MANAGER")
                .requesterUsername("nhanvien")
                .oneTimeToken(token)
                .tokenExpiresAt(java.time.LocalDateTime.now().plusHours(24))
                .build();
        requestRepository.save(apr);

        // Gọi endpoint public không cần authentication
        mockMvc().perform(get("/api/workflow/approvals/quick-token-decision")
                        .param("token", token)
                        .param("action", "APPROVE")
                        .param("reason", "Phê duyệt nhanh từ nút bấm Email Notification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("APPROVED")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("6. Tra cứu sổ nhật ký kiểm toán bất biến (Immutable Audit Trail)")
    void testImmutableAuditLogTrail() throws Exception {
        mockMvc().perform(get("/api/workflow/audit-logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("7. Thống kê Dashboard phê duyệt (Stats count)")
    void testApprovalStatistics() throws Exception {
        mockMvc().perform(get("/api/workflow/approvals/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending", notNullValue()))
                .andExpect(jsonPath("$.approved", notNullValue()))
                .andExpect(jsonPath("$.total", notNullValue()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("8. Quản trị ma trận quy tắc: Thêm mới quy tắc ma trận và truy vấn danh sách")
    void testCreateAndQueryApprovalMatrixRule() throws Exception {
        ApprovalMatrixRuleDto ruleDto = ApprovalMatrixRuleDto.builder()
                .ruleName("Chiết khấu đại lý VIP > 10 triệu [CEO duyệt]")
                .documentType(ApprovalDocumentType.SPECIAL_DISCOUNT)
                .minAmount(new BigDecimal("10000000.00"))
                .maxAmount(null)
                .level(1)
                .approverRole("ROLE_ADMIN")
                .autoApprove(false)
                .priority(5)
                .active(true)
                .build();

        mockMvc().perform(post("/api/workflow/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName", is("Chiết khấu đại lý VIP > 10 triệu [CEO duyệt]")))
                .andExpect(jsonPath("$.approverRole", is("ROLE_ADMIN")));

        mockMvc().perform(get("/api/workflow/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }
}
