package de.htwg.in.schneider.easygather.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProductsWithoutTokenIsAllowed() throws Exception {
        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk());
    }

    @Test
    void getCategoriesWithoutTokenIsAllowed() throws Exception {
        mockMvc.perform(get("/api/category"))
                .andExpect(status().isOk());
    }
}
