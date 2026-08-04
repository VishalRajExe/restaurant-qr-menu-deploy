package com.restaurantqr.platform.modules.media.storage;

import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageProvider {

    MediaAsset.StorageProviderType getProviderType();

    String uploadImage(MultipartFile file, String subfolder) throws IOException;

    String uploadBytes(byte[] bytes, String publicId) throws IOException;

    void deleteFile(String publicId) throws IOException;

    String generateCdnUrl(String publicId, int width, int height, String cropMode);
}
