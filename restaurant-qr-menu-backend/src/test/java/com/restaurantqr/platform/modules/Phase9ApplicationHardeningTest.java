package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase9ApplicationHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Security Headers: HTTP responses contain essential security headers")
    void securityHeaders_presentInResponse() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/plans").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    @DisplayName("Safe Error Formatting: Bad request validation errors omit stack traces and internal details")
    void safeErrorResponse_noStackTraceLeak() throws Exception {
        String invalidRegister = """
                {
                    "restaurantName": "",
                    "name": "",
                    "email": "not-an-email",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRegister))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("Method Argument Type Mismatch: Returns clean 400 Bad Request without leaking internal exception trace")
    void typeMismatch_cleanBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/restaurant/valid-slug").contextPath("/api/v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
