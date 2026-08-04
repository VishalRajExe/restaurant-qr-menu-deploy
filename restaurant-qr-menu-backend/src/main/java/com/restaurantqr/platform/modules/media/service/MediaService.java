package com.restaurantqr.platform.modules.media.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import com.restaurantqr.platform.modules.media.repository.MediaAssetRepository;
import com.restaurantqr.platform.modules.media.storage.CloudinaryStorageProvider;
import com.restaurantqr.platform.modules.media.storage.S3StorageProvider;
import com.restaurantqr.platform.modules.media.storage.StorageProvider;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final RestaurantService restaurantService;
    private final CloudinaryStorageProvider cloudinaryStorageProvider;
    private final S3StorageProvider s3StorageProvider;
    private final ImageProcessingService imageProcessingService;

    @Value("${media.storage.provider:CLOUDINARY}")
    private String activeProvider;

    public StorageProvider getActiveStorageProvider() {
        if ("S3".equalsIgnoreCase(activeProvider)) {
            return s3StorageProvider;
        }
        return cloudinaryStorageProvider;
    }

    @Transactional
    public MediaAsset upload(Long restaurantId, MultipartFile file, String folder) throws IOException {
        var restaurant = restaurantService.findById(restaurantId);
        StorageProvider provider = getActiveStorageProvider();

        byte[] rawBytes = file.getBytes();
        byte[] webpBytes = imageProcessingService.compressToWebp(rawBytes);
        var dimensions = imageProcessingService.extractDimensions(rawBytes);

        String publicId = (folder != null ? folder : "gallery") + "/" + UUID.randomUUID();
        String url = provider.uploadImage(file, folder);

        var asset = MediaAsset.builder()
                .restaurant(restaurant)
                .url(url)
                .publicId(publicId)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.webp")
                .fileType("image/webp")
                .fileSize((long) webpBytes.length)
                .width(dimensions.getWidth())
                .height(dimensions.getHeight())
                .provider(provider.getProviderType())
                .build();

        return mediaAssetRepository.save(asset);
    }

    @Transactional
    public List<MediaAsset> uploadMultiple(Long restaurantId, List<MultipartFile> files, String folder) throws IOException {
        List<MediaAsset> assets = new ArrayList<>();
        for (MultipartFile file : files) {
            assets.add(upload(restaurantId, file, folder));
        }
        return assets;
    }

    public List<MediaAsset> getRestaurantGallery(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return mediaAssetRepository.findGalleryByRestaurantId(restaurantId);
    }

    @Transactional
    public MediaAsset cropAsset(Long restaurantId, Long assetId, int x, int y, int width, int height) throws IOException {
        var asset = mediaAssetRepository.findByIdAndRestaurantIdAndIsDeletedFalse(assetId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaAsset", assetId));

        String cropMeta = String.format("x=%d,y=%d,w=%d,h=%d", x, y, width, height);
        asset.setCropData(cropMeta);
        asset.setWidth(width);
        asset.setHeight(height);

        return mediaAssetRepository.save(asset);
    }

    public String getCdnUrl(Long restaurantId, Long assetId, int width, int height, String cropMode) {
        var asset = mediaAssetRepository.findByIdAndRestaurantIdAndIsDeletedFalse(assetId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaAsset", assetId));

        StorageProvider provider = getActiveStorageProvider();
        return provider.generateCdnUrl(asset.getPublicId(), width, height, cropMode);
    }

    @Transactional
    public void deleteAsset(Long restaurantId, Long assetId) throws IOException {
        var asset = mediaAssetRepository.findByIdAndRestaurantIdAndIsDeletedFalse(assetId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("MediaAsset", assetId));

        getActiveStorageProvider().deleteFile(asset.getPublicId());
        asset.softDelete();
        mediaAssetRepository.save(asset);
    }
}
