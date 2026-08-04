package com.restaurantqr.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.restaurantqr.common.BadRequestException;
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

    // Upload MultipartFile
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file, String subfolder) throws IOException {

        validateImageFile(file);

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", folder + "/" + subfolder,
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

        Map<String, Object> params = ObjectUtils.asMap(
                "public_id", folder + "/" + publicId,
                "resource_type", "image",
                "overwrite", true
        );

        Map<String, Object> result =
                cloudinary.uploader().upload(bytes, params);

        return (String) result.get("secure_url");
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

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new BadRequestException(
                    "Only image files are allowed"
            );
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