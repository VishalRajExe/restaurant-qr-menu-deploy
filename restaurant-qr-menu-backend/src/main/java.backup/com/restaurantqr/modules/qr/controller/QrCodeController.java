package com.restaurantqr.modules.qr.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.modules.qr.entity.QrCode;
import com.restaurantqr.modules.qr.service.QrCodeRequest;
import com.restaurantqr.modules.qr.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;

    // Public: customer scans → resolve QR token → get restaurant data
    @GetMapping("/public/qr/{token}")
    public ResponseEntity<ApiResponse<QrCode>> resolveQr(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(qrCodeService.scan(token)));
    }

    // Admin: generate
    @PostMapping("/restaurants/{restaurantId}/qr-codes")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<QrCode>> generate(
            @PathVariable Long restaurantId,
            @Valid @RequestBody QrCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("QR Code generated", qrCodeService.generate(restaurantId, request)));
    }

    @GetMapping("/restaurants/{restaurantId}/qr-codes")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<QrCode>>> list(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(qrCodeService.findByRestaurant(restaurantId)));
    }

    @PatchMapping("/restaurants/{restaurantId}/qr-codes/{id}/deactivate")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long restaurantId,
                                                         @PathVariable Long id) {
        qrCodeService.deactivate(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("QR code deactivated", null));
    }

    @DeleteMapping("/restaurants/{restaurantId}/qr-codes/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long restaurantId,
                                                     @PathVariable Long id) {
        qrCodeService.delete(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("QR code deleted", null));
    }
}
