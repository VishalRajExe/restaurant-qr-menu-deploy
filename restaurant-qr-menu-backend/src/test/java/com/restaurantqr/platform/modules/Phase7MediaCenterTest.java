package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import com.restaurantqr.platform.modules.media.service.MediaService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
@org.springframework.test.annotation.DirtiesContext
class Phase7MediaCenterTest {


    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaService mediaService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.cloudinary.Cloudinary cloudinary;

    private Restaurant testRestaurant;
    private User ownerUser;

    @BeforeEach
    void setUp() throws IOException {
        com.cloudinary.Uploader mockUploader = org.mockito.Mockito.mock(com.cloudinary.Uploader.class);
        org.mockito.Mockito.when(cloudinary.uploader()).thenReturn(mockUploader);
        org.mockito.Mockito.when(mockUploader.upload(org.mockito.ArgumentMatchers.any(byte[].class), org.mockito.ArgumentMatchers.any(java.util.Map.class)))
                .thenReturn(java.util.Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v12345/test.webp"));

        com.cloudinary.Url mockUrl = org.mockito.Mockito.mock(com.cloudinary.Url.class);
        org.mockito.Mockito.when(cloudinary.url()).thenReturn(mockUrl);
        org.mockito.Mockito.when(mockUrl.transformation(org.mockito.ArgumentMatchers.any())).thenReturn(mockUrl);
        org.mockito.Mockito.when(mockUrl.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> "https://res.cloudinary.com/demo/image/upload/w_500,h_500,c_fill/" + inv.getArgument(0));

        RestaurantRequest req = new RestaurantRequest();
        req.name = "Media Center Bistro";
        req.slug = "media-bistro-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);


        ownerUser = userRepository.save(User.builder()
                .name("Media Owner")
                .email("mediaowner-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        JwtUserDetails details = new JwtUserDetails(ownerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("1. Single & Batch Drag-and-Drop Upload: Upload images to Cloud/CDN storage")
    void testSingleAndBatchUpload() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("file", "burger.png", "image/png", new byte[]{1, 2, 3, 4});
        MockMultipartFile file2 = new MockMultipartFile("file", "pizza.jpeg", "image/jpeg", new byte[]{5, 6, 7, 8});

        MediaAsset singleAsset = mediaService.upload(testRestaurant.getId(), file1, "items");
        assertNotNull(singleAsset.getId());
        assertNotNull(singleAsset.getUrl());
        assertEquals("image/webp", singleAsset.getFileType());

        List<MediaAsset> batchAssets = mediaService.uploadMultiple(testRestaurant.getId(), List.of(file1, file2), "gallery");
        assertEquals(2, batchAssets.size());
    }

    @Test
    @DisplayName("2. WebP Compression & Metadata: Extract width, height, and compressed file size")
    void testWebpCompressionAndDimensions() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "pasta.png", "image/png", new byte[]{10, 20, 30, 40, 50});
        MediaAsset asset = mediaService.upload(testRestaurant.getId(), file, "items");

        assertNotNull(asset.getWidth());
        assertNotNull(asset.getHeight());
        assertTrue(asset.getFileSize() > 0);
    }

    @Test
    @DisplayName("3. Restaurant Media Gallery: Retrieve all gallery assets for a restaurant")
    void testGalleryListing() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("file", "ambiance1.jpg", "image/jpeg", new byte[]{1, 2});
        MockMultipartFile file2 = new MockMultipartFile("file", "ambiance2.jpg", "image/jpeg", new byte[]{3, 4});

        mediaService.uploadMultiple(testRestaurant.getId(), List.of(file1, file2), "ambiance");

        List<MediaAsset> gallery = mediaService.getRestaurantGallery(testRestaurant.getId());
        assertEquals(2, gallery.size());
    }

    @Test
    @DisplayName("4. Image Crop & CDN URL: Crop image coordinates and generate dynamic CDN URL")
    void testImageCroppingAndCdnUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "banner.png", "image/png", new byte[]{10, 20});
        MediaAsset asset = mediaService.upload(testRestaurant.getId(), file, "banners");

        MediaAsset croppedAsset = mediaService.cropAsset(testRestaurant.getId(), asset.getId(), 10, 10, 400, 400);
        assertNotNull(croppedAsset.getCropData());
        assertEquals(400, croppedAsset.getWidth());

        String cdnUrl = mediaService.getCdnUrl(testRestaurant.getId(), asset.getId(), 500, 500, "fill");
        assertNotNull(cdnUrl);
        assertTrue(cdnUrl.contains("500"));
    }

    @Test
    @DisplayName("5. Delete Media Asset: Remove asset from storage provider and soft-delete record")
    void testAssetDeletion() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "temp.png", "image/png", new byte[]{1, 1, 1});
        MediaAsset asset = mediaService.upload(testRestaurant.getId(), file, "temp");

        mediaService.deleteAsset(testRestaurant.getId(), asset.getId());

        List<MediaAsset> gallery = mediaService.getRestaurantGallery(testRestaurant.getId());
        assertTrue(gallery.isEmpty());
    }
}
