package com.restaurantqr.platform.modules.media.storage;

import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageProvider implements StorageProvider {

    @Value("${aws.s3.bucket:restaurant-qr-bucket}")
    private String bucketName;

    @Value("${aws.s3.cdn-domain:https://cdn.restaurantqr.com}")
    private String cdnDomain;

    @Override
    public MediaAsset.StorageProviderType getProviderType() {
        return MediaAsset.StorageProviderType.S3;
    }

    @Override
    public String uploadImage(MultipartFile file, String subfolder) throws IOException {
        String safeSubfolder = subfolder != null ? subfolder.replaceAll("[^a-zA-Z0-9_-]", "") : "gallery";
        String filename = safeSubfolder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        log.info("Simulating Amazon S3 upload to bucket {}: {}", bucketName, filename);
        return cdnDomain + "/" + filename;
    }

    @Override
    public String uploadBytes(byte[] bytes, String publicId) throws IOException {
        log.info("Simulating Amazon S3 byte upload to bucket {}: {}", bucketName, publicId);
        return cdnDomain + "/" + publicId;
    }

    @Override
    public void deleteFile(String publicId) throws IOException {
        log.info("Simulating Amazon S3 file deletion from bucket {}: {}", bucketName, publicId);
    }

    @Override
    public String generateCdnUrl(String publicId, int width, int height, String cropMode) {
        String mode = cropMode != null ? cropMode : "fill";
        return cdnDomain + "/" + publicId + "?w=" + width + "&h=" + height + "&crop=" + mode + "&fmt=webp";
    }
}
