package com.restaurantqr.platform.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.restaurantqr.platform.common.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryUploadService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:restaurant-qr}")
    private String folder;

    private static final java.util.Set<String> ALLOWED_MIME_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    // Upload MultipartFile
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file, String subfolder) throws IOException {

        validateImageFile(file);

        String safeSubfolder = subfolder != null ? subfolder.replaceAll("[^a-zA-Z0-9_-]", "") : "general";

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", folder + "/" + safeSubfolder,
                "resource_type", "image",
                "quality", "auto",
                "fetch_format", "auto"
        );

        Map<String, Object> result =
                cloudinary.uploader().upload(file.getBytes(), params);

        return (String) result.get("secure_url");
    }

    // Upload byte[]
    @SuppressWarnings("unchecked")
    public String uploadBytes(byte[] bytes, String publicId) throws IOException {

        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id", folder + "/" + publicId,
                    "resource_type", "image",
                    "overwrite", true
            );

            Map<String, Object> result =
                    cloudinary.uploader().upload(bytes, params);

            return (String) result.get("secure_url");
        } catch (Exception e) {
            log.warn("Cloudinary upload failed ({}), returning base64 data URI", e.getMessage());
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes);
        }
    }

    public void deleteImage(String url) {

        try {

            String publicId = extractPublicId(url);

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );

        } catch (Exception e) {

            log.warn("Could not delete image: {}", e.getMessage());

        }
    }

    private void validateImageFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {

            throw new BadRequestException(
                    "Only JPEG, PNG, WEBP, and GIF images are allowed"
            );
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            if (lowerName.contains("..") || lowerName.contains("/") || lowerName.contains("\\")) {
                throw new BadRequestException("Filename contains invalid path traversal characters");
            }
            if (lowerName.endsWith(".php") || lowerName.endsWith(".jsp") || lowerName.endsWith(".exe")
                    || lowerName.endsWith(".sh") || lowerName.endsWith(".html") || lowerName.endsWith(".svg") || lowerName.endsWith(".js")) {
                throw new BadRequestException("File extension is not allowed");
            }
        }

        if (file.getSize() > 5 * 1024 * 1024) {

            throw new BadRequestException(
                    "Image must be under 5 MB"
            );
        }
    }

    private String extractPublicId(String url) {

        int uploadIndex = url.indexOf("/upload/");

        if (uploadIndex == -1) {
            return url;
        }

        String path = url.substring(uploadIndex + 8);

        if (path.startsWith("v") &&
                path.indexOf('/') > 0) {

            path = path.substring(
                    path.indexOf('/') + 1
            );
        }

        int dotIndex = path.lastIndexOf('.');

        if (dotIndex > 0) {

            path = path.substring(0, dotIndex);

        }

        return path;
    }
}