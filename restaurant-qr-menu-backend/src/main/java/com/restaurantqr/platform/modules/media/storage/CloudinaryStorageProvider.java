package com.restaurantqr.platform.modules.media.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.restaurantqr.platform.config.CloudinaryUploadService;
import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageProvider implements StorageProvider {

    private final CloudinaryUploadService cloudinaryUploadService;
    private final Cloudinary cloudinary;

    @Override
    public MediaAsset.StorageProviderType getProviderType() {
        return MediaAsset.StorageProviderType.CLOUDINARY;
    }

    @Override
    public String uploadImage(MultipartFile file, String subfolder) throws IOException {
        return cloudinaryUploadService.uploadImage(file, subfolder);
    }

    @Override
    public String uploadBytes(byte[] bytes, String publicId) throws IOException {
        return cloudinaryUploadService.uploadBytes(bytes, publicId);
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        cloudinaryUploadService.deleteImage(publicId);
    }

    @Override
    public String generateCdnUrl(String publicId, int width, int height, String cropMode) {
        String crop = cropMode != null ? cropMode : "fill";
        return cloudinary.url()
                .transformation(new Transformation<>().width(width).height(height).crop(crop).quality("auto").fetchFormat("webp"))
                .generate(publicId);
    }
}
