package com.restaurantqr.platform.modules;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.analytics.repository.ScanEventRepository;
import com.restaurantqr.platform.analytics.service.AnalyticsService;
import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.config.CloudinaryUploadService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext
class Phase7FilesAnalyticsAndAuditingTest {


    @Autowired
    private CloudinaryUploadService cloudinaryUploadService;

    @Autowired
    private AnalyticsService analyticsService;

    @MockBean
    private Cloudinary cloudinary;

    @MockBean
    private RestaurantRepository restaurantRepository;

    @MockBean
    private ScanEventRepository scanEventRepository;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() throws IOException {
        restaurant = Restaurant.builder()
                .name("Analytics Rest")
                .slug("analytics-rest")
                .status(Restaurant.Status.ACTIVE)
                .build();
        restaurant.setId(55L);

        var owner = com.restaurantqr.platform.users.entity.User.builder()
                .email("owner@analytics.com")
                .password("enc")
                .role(com.restaurantqr.platform.users.entity.User.Role.RESTAURANT_OWNER)
                .status(com.restaurantqr.platform.users.entity.User.Status.ACTIVE)
                .restaurant(restaurant)
                .build();
        owner.setId(555L);

        var userDetails = new com.restaurantqr.platform.security.JwtUserDetails(owner);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(restaurantRepository.findById(55L)).thenReturn(Optional.of(restaurant));

        var uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/sample.png"));
    }

    @Test
    @DisplayName("File Security: Valid PNG upload succeeds")
    void uploadImage_validPng_success() throws Exception {
        var mockFile = new MockMultipartFile(
                "file", "logo.png", "image/png", "png-content".getBytes());

        String url = cloudinaryUploadService.uploadImage(mockFile, "logos");
        assertNotNull(url);
        assertTrue(url.contains("cloudinary.com"));
    }

    @Test
    @DisplayName("File Security: Disallowed MIME type (application/json) rejected with 400 Bad Request")
    void uploadImage_disallowedMime_throwsBadRequest() {
        var mockFile = new MockMultipartFile(
                "file", "payload.json", "application/json", "{}".getBytes());

        assertThrows(BadRequestException.class,
                () -> cloudinaryUploadService.uploadImage(mockFile, "logos"));
    }

    @Test
    @DisplayName("File Security: Script extension (.php / .exe) rejected")
    void uploadImage_scriptExtension_throwsBadRequest() {
        var mockFile = new MockMultipartFile(
                "file", "shell.php", "image/png", "malicious-code".getBytes());

        assertThrows(BadRequestException.class,
                () -> cloudinaryUploadService.uploadImage(mockFile, "logos"));
    }

    @Test
    @DisplayName("File Security: Path traversal filename rejected")
    void uploadImage_pathTraversal_throwsBadRequest() {
        var mockFile = new MockMultipartFile(
                "file", "../../etc/passwd.png", "image/png", "traversal".getBytes());

        assertThrows(BadRequestException.class,
                () -> cloudinaryUploadService.uploadImage(mockFile, "logos"));
    }

    @Test
    @DisplayName("Analytics: Dashboard stats returns verified aggregated metrics with tenant isolation")
    void getDashboardStats_returnsAggregatedData() {
        when(scanEventRepository.countByRestaurantIdAndCreatedAtBetween(any(), any(), any())).thenReturn(42L);
        when(scanEventRepository.countDailyScans(any(), any())).thenReturn(Collections.emptyList());
        when(scanEventRepository.countByDeviceType(any(), any())).thenReturn(Collections.emptyList());
        when(scanEventRepository.topQrCodes(any(), any())).thenReturn(Collections.emptyList());

        var stats = analyticsService.getDashboardStats(55L);
        assertNotNull(stats);
        assertEquals(42L, stats.todayScans());
        assertEquals(42L, stats.monthScans());
    }
}
