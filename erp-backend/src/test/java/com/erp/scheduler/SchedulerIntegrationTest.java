package com.erp.scheduler;

import com.erp.scheduler.model.ScheduledJob;
import com.erp.scheduler.repository.ScheduledJobRepository;
import com.erp.scheduler.service.DynamicSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchedulerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ScheduledJobRepository scheduledJobRepository;

    @Autowired
    private DynamicSchedulerService schedulerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("TC01 - Lấy danh sách tác vụ tự động và kiểm tra các job mặc định")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetAllJobsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/scheduler/jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].jobCode", hasItem("JOB_DAILY_SALES")));
    }

    @Test
    @DisplayName("TC02 - Kích hoạt chạy ngay (Trigger Now) và lưu nhật ký thực thi")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldTriggerJobAndGenerateExecutionLog() throws Exception {
        ScheduledJob job = scheduledJobRepository.findByJobCode("JOB_DAILY_SALES")
                .orElseThrow(() -> new IllegalStateException("Job JOB_DAILY_SALES không tồn tại"));

        // 1. Kiểm tra API Trigger Now phản hồi lệnh thành công
        mockMvc.perform(post("/api/scheduler/jobs/" + job.getId() + "/trigger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Đã phát lệnh kích hoạt chạy tác vụ")));

        // 2. Chạy tác vụ trực tiếp để kiểm tra logic tổng hợp báo cáo và lưu Log
        String summary = schedulerService.executeTaskWrapped(job.getId(), "ADMIN_MANUAL");
        assertThat(summary).contains("Doanh thu:").contains("Định giá kho:");

        // 3. Kiểm tra log thực thi đã được ghi nhận trong cơ sở dữ liệu
        mockMvc.perform(get("/api/scheduler/logs")
                        .param("jobCode", "JOB_DAILY_SALES")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].status", is("SUCCESS")));
    }
}
