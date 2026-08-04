package com.restaurantqr.platform.modules.qr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.restaurantqr.platform.common.*;
import com.restaurantqr.platform.config.CloudinaryUploadService;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final QrCodeRepository qrCodeRepository;
    private final BranchRepository branchRepository;
    private final RestaurantService restaurantService;
    private final CloudinaryUploadService cloudinaryUploadService;

    @Value("${qr.base-url}")
    private String qrBaseUrl;

    @Value("${qr.image-width:300}")
    private int imageWidth;

    @Value("${qr.image-height:300}")
    private int imageHeight;

    // ─── Generate QR Code ─────────────────────────────────────────────────────

    @Transactional
    public QrCode generate(Long restaurantId, QrCodeRequest request) {
        var restaurant = restaurantService.findById(restaurantId);
        var branch = branchRepository.findById(request.branchId)
                .filter(b -> b.getRestaurant().getId().equals(restaurantId) && !b.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", request.branchId));

        String token = UUID.randomUUID().toString().replace("-", "");
        String menuUrl = String.format("%s/%s?table=%s",
                qrBaseUrl, token, request.tableNumber != null ? request.tableNumber : "");

        try {
            byte[] qrImageBytes = generateQrImage(menuUrl);
            String cloudinaryUrl = cloudinaryUploadService.uploadBytes(qrImageBytes,
                    "qrcodes/" + restaurantId + "/" + token);

            var qrCode = QrCode.builder()
                    .branch(branch)
                    .restaurant(restaurant)
                    .tableNumber(request.tableNumber)
                    .label(request.label)
                    .token(token)
                    .qrImageUrl(cloudinaryUrl)
                    .build();

            var saved = qrCodeRepository.save(qrCode);
            log.info("QR Code generated for restaurant={} branch={} table={}",
                    restaurantId, request.branchId, request.tableNumber);
            return saved;

        } catch (IOException e) {
            throw new BadRequestException("Failed to generate QR code: " + e.getMessage());
        }
    }

    // ─── Scan (public, increments counter) ───────────────────────────────────

    @Transactional
    public QrCode scan(String token) {
        var qrCode = qrCodeRepository.findByTokenAndStatus(token, QrCode.Status.ACTIVE)
                .filter(q -> !q.getIsDeleted()
                        && q.getRestaurant() != null
                        && !q.getRestaurant().getIsDeleted()
                        && q.getRestaurant().getStatus() == com.restaurantqr.platform.modules.restaurant.entity.Restaurant.Status.ACTIVE
                        && (q.getBranch() == null || !q.getBranch().getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("QR code not found or inactive"));
        qrCode.incrementScan();
        return qrCodeRepository.save(qrCode);
    }

    public java.util.Optional<QrCode> findByToken(String token) {
        return qrCodeRepository.findByToken(token);
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────


    public QrCode findById(Long id, Long restaurantId) {
        restaurantService.findById(restaurantId);
        var qrCode = qrCodeRepository.findById(id)
                .filter(q -> !q.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("QrCode", id));
        if (!qrCode.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("Not your QR code");
        }
        return qrCode;
    }

    public QrCode findById(Long id) {
        return qrCodeRepository.findById(id)
                .filter(q -> !q.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("QrCode", id));
    }

    public List<QrCode> findByRestaurant(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return qrCodeRepository.findByRestaurantId(restaurantId);
    }

    public List<QrCode> findByBranch(Long branchId) {
        var branch = branchRepository.findById(branchId)
                .filter(b -> !b.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch", branchId));
        restaurantService.findById(branch.getRestaurant().getId());
        return qrCodeRepository.findByBranchId(branchId);
    }

    @Transactional
    public void deactivate(Long id, Long restaurantId) {
        var qrCode = findById(id, restaurantId);
        qrCode.setStatus(QrCode.Status.INACTIVE);
        qrCodeRepository.save(qrCode);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var qrCode = findById(id, restaurantId);
        qrCode.softDelete();
        qrCodeRepository.save(qrCode);
    }

    // ─── QR Image Generation (ZXing) ─────────────────────────────────────────

    private byte[] generateQrImage(String content) throws IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, imageWidth, imageHeight, hints);

            // White background, black modules
            MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, config);

            // Add small logo/padding frame (optional branding)
            BufferedImage finalImage = addBranding(qrImage);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalImage, "PNG", baos);
            return baos.toByteArray();

        } catch (WriterException e) {
            throw new IOException("ZXing QR generation failed: " + e.getMessage(), e);
        }
    }

    private BufferedImage addBranding(BufferedImage qrImage) {
        // Add 10px white border around QR for cleaner look
        int border = 10;
        int w = qrImage.getWidth() + border * 2;
        int h = qrImage.getHeight() + border * 2;

        BufferedImage branded = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = branded.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(qrImage, border, border, null);
        g.dispose();
        return branded;
    }
}
