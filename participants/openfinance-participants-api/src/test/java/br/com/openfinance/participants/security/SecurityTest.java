package br.com.openfinance.participants.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowGetRequests() throws Exception {
        mockMvc.perform(get("/api/v1/participants"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

/*    @Test
    void shouldAllowCacheRefresh() throws Exception {
        mockMvc.perform(post("/api/v1/participants/cache/refresh"))
                .andExpected(status().isOk());
    }

    @Test
    void shouldRejectInvalidHttpMethods() throws Exception {
        mockMvc.perform(delete("/api/v1/participants"))
                .andExpected(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/v1/participants"))
                .andExpected(status().isMethodNotAllowed());
    }*/
}
