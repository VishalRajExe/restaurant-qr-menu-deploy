package com.restaurantqr.platform.modules.media.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ImageProcessingService {

    @Data
    @Builder
    public static class ImageDimensions {
        private int width;
        private int height;
        private String format;
    }

    public ImageDimensions extractDimensions(byte[] imageBytes) {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                return ImageDimensions.builder().width(800).height(600).format("WEBP").build();
            }
            return ImageDimensions.builder()
                    .width(image.getWidth())
                    .height(image.getHeight())
                    .format("WEBP")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to extract image dimensions: {}", e.getMessage());
            return ImageDimensions.builder().width(800).height(600).format("WEBP").build();
        }
    }

    public byte[] compressToWebp(byte[] imageBytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage originalImage = ImageIO.read(is);
            if (originalImage == null) {
                return imageBytes;
            }

            // Create WebP/RGB buffered image
            BufferedImage resizedImage = new BufferedImage(
                    originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(resizedImage, "jpg", baos);
            if (!written) {
                return imageBytes;
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("WebP compression fallback to raw bytes: {}", e.getMessage());
            return imageBytes;
        }
    }

    public byte[] cropImage(byte[] imageBytes, int x, int y, int cropWidth, int cropHeight) throws IOException {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage originalImage = ImageIO.read(is);
            if (originalImage == null) {
                return imageBytes;
            }

            int validX = Math.max(0, Math.min(x, originalImage.getWidth() - 1));
            int validY = Math.max(0, Math.min(y, originalImage.getHeight() - 1));
            int validW = Math.min(cropWidth, originalImage.getWidth() - validX);
            int validH = Math.min(cropHeight, originalImage.getHeight() - validY);

            BufferedImage cropped = originalImage.getSubimage(validX, validY, validW, validH);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(cropped, "jpg", baos);
            return baos.toByteArray();
        }
    }
}
