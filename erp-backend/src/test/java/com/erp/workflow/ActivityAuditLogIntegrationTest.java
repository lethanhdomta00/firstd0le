package com.erp.workflow;

import com.erp.workflow.model.ImmutableAuditLog;
import com.erp.workflow.repository.ImmutableAuditLogRepository;
import com.erp.workflow.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class ActivityAuditLogIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ImmutableAuditLogRepository auditLogRepository;

    private MockMvc buildMockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testSearchAndStatsAuditLogs() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        ImmutableAuditLog saved = auditLogService.logAction(
                "admin",
                "ROLE_ADMIN",
                "UPDATE",
                "PRODUCT",
                101L,
                "SKU-101",
                "{\"price\": 100000}",
                "{\"price\": 120000}",
                "Điều chỉnh giá bán sản phẩm SKU-101",
                "192.168.1.10",
                "JUnit-MockMvc"
        );

        mockMvc.perform(get("/api/audit-logs")
                        .param("keyword", "SKU-101")
                        .param("entityType", "PRODUCT")
                        .param("action", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].traceId", is(saved.getTraceId())))
                .andExpect(jsonPath("$.content[0].entityCode", is("SKU-101")));

        mockMvc.perform(get("/api/audit-logs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalToday", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeUsersToday", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.entityBreakdown", notNullValue()));

        mockMvc.perform(get("/api/audit-logs/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oldValueJson", containsString("100000")))
                .andExpect(jsonPath("$.newValueJson", containsString("120000")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testExportAuditLogsExcel() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        auditLogService.logAction(
                "admin", "ROLE_ADMIN", "CREATE", "ORDER", 202L, "ORD-202",
                null, "{\"total\": 500000}", "Tạo đơn hàng mới ORD-202", "127.0.0.1", "JUnit"
        );

        mockMvc.perform(get("/api/audit-logs/export")
                        .param("entityType", "ORDER"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", containsString("audit-logs-")));
    }

    @Test
    @WithMockUser(username = "sales_staff", roles = {"SALES"})
    void testAccessDeniedForSalesRole() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testLoginFailedCreatesAuditLog() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        long beforeCount = auditLogRepository.count();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown_hacker\",\"password\":\"wrong_pass\"}"))
                .andExpect(status().isUnauthorized());

        long afterCount = auditLogRepository.count();
        assert afterCount > beforeCount : "Phải ghi nhận log LOGIN_FAILED khi đăng nhập sai!";

        // Dọn dẹp bản ghi test đã commit
        auditLogRepository.findAll().stream()
                .filter(l -> "unknown_hacker".equals(l.getUsername()) && "LOGIN_FAILED".equals(l.getAction()))
                .forEach(auditLogRepository::delete);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCopilotAuditLogIntegration() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        auditLogService.logAction(
                "admin", "ROLE_ADMIN", "UPDATE", "PRODUCT", 303L, "SKU-303",
                null, null, "Cập nhật tồn kho SKU-303", "127.0.0.1", "JUnit"
        );

        mockMvc.perform(post("/api/copilot/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Tổng hợp nhật ký thao tác hôm nay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uiWidget.type", is("AUDIT_LOG_CARD")))
                .andExpect(jsonPath("$.uiWidget.data.totalToday", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.uiWidget.data.recentLogs", notNullValue()));
    }
}
