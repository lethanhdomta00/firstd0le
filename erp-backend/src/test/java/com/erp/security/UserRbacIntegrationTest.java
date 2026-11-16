package com.erp.security;

import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class UserRbacIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUpSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(30) NOT NULL DEFAULT 'USER'");
        } catch (Exception ignored) {
        }
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. Load User Details should populate both ROLE_ and detailed permissions")
    void testLoadUserDetailsAuthorities() {
        User accountant = new User();
        accountant.setUsername("test_accountant");
        accountant.setPassword("password123");
        accountant.setRole(Role.ACCOUNTANT);
        accountant.setStatus("ACTIVE");
        userRepository.save(accountant);

        UserDetails details = userDetailsService.loadUserByUsername("test_accountant");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ACCOUNTANT", "ORDER_VIEW", "ORDER_PAY", "ORDER_EXPORT", "ANALYTICS_VIEW");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("2. GET /api/users/roles-matrix should return 6 roles with full permissions metadata")
    void testGetRolesMatrix() throws Exception {
        mockMvc().perform(get("/api/users/roles-matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.role == 'ADMIN')].title").value("Quản trị viên tối cao"))
                .andExpect(jsonPath("$[?(@.role == 'ACCOUNTANT')].title").value("Kế toán tài chính"))
                .andExpect(jsonPath("$[?(@.role == 'WAREHOUSE')].title").value("Thủ kho"))
                .andExpect(jsonPath("$[?(@.role == 'SALES')].title").value("Nhân viên kinh doanh"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("3. POST /api/users should allow ADMIN to create user with specialized role")
    void testCreateUserWithSpecializedRole() throws Exception {
        String payload = """
            {
                "username": "warehouse_keeper_01",
                "password": "password123",
                "email": "warehouse01@erp.com",
                "role": "WAREHOUSE",
                "status": "ACTIVE"
            }
        """;

        mockMvc().perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("warehouse_keeper_01"))
                .andExpect(jsonPath("$.role").value("WAREHOUSE"))
                .andExpect(jsonPath("$.roleTitle").value("Thủ kho"));

        assertThat(userRepository.findByUsername("warehouse_keeper_01")).isPresent();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("4. PUT /api/users/roles-matrix should update dynamic permissions for roles")
    void testUpdateRolesMatrix() throws Exception {
        String updatePayload = """
            {
                "ACCOUNTANT": ["ORDER_READ", "ORDER_PAY", "ORDER_EXPORT", "ANALYTICS_VIEW"],
                "WAREHOUSE": ["PRODUCT_VIEW", "PRODUCT_STOCK", "ORDER_READ"]
            }
        """;

        mockMvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/users/roles-matrix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("5. PATCH /api/users/{id}/role should quickly change user role")
    void testQuickChangeUserRole() throws Exception {
        User staff = new User();
        staff.setUsername("quick_role_user");
        staff.setPassword("password123");
        staff.setRole(Role.USER);
        staff.setStatus("ACTIVE");
        userRepository.save(staff);

        String payload = "{\"role\":\"ACCOUNTANT\"}";

        mockMvc().perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/users/" + staff.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ACCOUNTANT"));

        User reloaded = userRepository.findById(staff.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.ACCOUNTANT);
    }
}
