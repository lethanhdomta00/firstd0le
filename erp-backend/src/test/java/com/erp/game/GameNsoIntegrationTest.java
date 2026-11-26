package com.erp.game;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class GameNsoIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetGameNsoOverviewAndLists() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(get("/api/game-nso/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverHost", notNullValue()))
                .andExpect(jsonPath("$.totalUsers", notNullValue()));

        mvc.perform(get("/api/game-nso/users").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        mvc.perform(get("/api/game-nso/players").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        mvc.perform(get("/api/game-nso/items").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        mvc.perform(get("/api/game-nso/clans").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        mvc.perform(get("/api/game-nso/icons/188"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/game-nso/players/1/bag"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "sales_staff", roles = {"SALES"})
    void testSalesUserForbiddenForAdminGameManagement() throws Exception {
        MockMvc mvc = mockMvc();

        mvc.perform(get("/api/game-nso/status"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/game-nso/users"))
                .andExpect(status().isForbidden());
    }
}
