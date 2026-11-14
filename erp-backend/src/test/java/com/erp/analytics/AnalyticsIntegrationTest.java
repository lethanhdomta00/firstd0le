package com.erp.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AnalyticsIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getOverview_returns200AndValidStructure() throws Exception {
        mockMvc().perform(get("/api/analytics/overview")
                        .param("fromDate", "2026-11-01")
                        .param("toDate", "2026-11-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.inventoryValuation").exists())
                .andExpect(jsonPath("$.revenueTrends").isArray())
                .andExpect(jsonPath("$.topSellingProducts").isArray())
                .andExpect(jsonPath("$.lowStockAlerts").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void exportExcel_returnsExcelFileStream() throws Exception {
        mockMvc().perform(get("/api/analytics/export-excel")
                        .param("fromDate", "2026-11-01")
                        .param("toDate", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ERP_Analytics_")))
                .andExpect(content().contentType("application/vnd.ms-excel;charset=UTF-8"));
    }
}
