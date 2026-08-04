package com.restaurantqr.platform.modules.offer.service;

import com.restaurantqr.platform.modules.offer.entity.Offer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OfferRequest {
    @NotBlank(message = "Offer title is required")
    public String title;
    public String description;
    @NotNull(message = "Discount type is required")
    public Offer.DiscountType discountType;
    public BigDecimal discountPercentage;
    public BigDecimal discountAmount;
    @NotNull(message = "Start date is required")
    public LocalDate startDate;
    @NotNull(message = "End date is required")
    public LocalDate endDate;
}
