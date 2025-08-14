package br.com.openfinance.participants.documentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldDocumentParticipantsApi() throws Exception {
        mockMvc.perform(get("/api/v1/participants"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldDocumentParticipantByCnpjApi() throws Exception {
        mockMvc.perform(get("/api/v1/participants/12345678000195"))
                .andDo(print());
    }

    @Test
    void shouldDocumentApiEndpointsApi() throws Exception {
        mockMvc.perform(get("/api/v1/participants/12345678000195/endpoints")
                        .param("apiFamily", "accounts"))
                .andDo(print());
    }
}
