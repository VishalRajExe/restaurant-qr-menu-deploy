package com.restaurantqr.platform.modules.qr.service;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QrCodeRequest {
    @NotNull
    public Long branchId;
    public String tableNumber;
    public String label;
}
