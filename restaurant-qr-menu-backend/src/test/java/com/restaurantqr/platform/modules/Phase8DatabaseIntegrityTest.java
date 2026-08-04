package com.restaurantqr.platform.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.common.ConflictException;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase8DatabaseIntegrityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    private Restaurant sampleRestaurant;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleRestaurant = Restaurant.builder()
                .name("DB Test Rest")
                .slug("db-test-rest")
                .status(Restaurant.Status.ACTIVE)
                .build();
        sampleRestaurant.setId(77L);

        sampleCategory = Category.builder()
                .name("Starters")
                .restaurant(sampleRestaurant)
                .build();
        sampleCategory.setId(770L);
    }

    @Test
    @DisplayName("DB Integrity: DataIntegrityViolationException is handled and returns HTTP 409 Conflict")
    void dataIntegrityViolation_returns409Conflict() throws Exception {
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation on email"));

        String registerBody = """
                {
                    "restaurantName": "New Rest",
                    "name": "John Doe",
                    "email": "existing@example.com",
                    "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("JPA Serialization Safety: Verify Category entity serializes without circular reference")
    void categorySerialization_noCircularReference() throws Exception {
        String json = objectMapper.writeValueAsString(sampleCategory);
        assertNotNull(json);
        assertTrue(json.contains("Starters"));
        assertFalse(json.contains("db-test-rest")); // Restaurant ignored
    }

    @Test
    @DisplayName("JPA Serialization Safety: Verify MenuItem entity serializes without circular reference")
    void menuItemSerialization_noCircularReference() throws Exception {
        var item = MenuItem.builder()
                .name("Burger")
                .price(new BigDecimal("12.99"))
                .category(sampleCategory)
                .restaurant(sampleRestaurant)
                .build();
        item.setId(999L);

        String json = objectMapper.writeValueAsString(item);
        assertNotNull(json);
        assertTrue(json.contains("Burger"));
        assertFalse(json.contains("db-test-rest")); // Restaurant ignored
    }
}
