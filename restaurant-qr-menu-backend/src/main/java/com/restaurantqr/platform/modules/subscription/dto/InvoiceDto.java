package com.restaurantqr.platform.modules.subscription.dto;

import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {

    private Long subscriptionId;
    private String invoiceNumber;
    private Long restaurantId;
    private String restaurantName;
    private Subscription.Plan plan;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount; // GST (18%)
    private BigDecimal totalAmountPaid;
    private String currency;
    private String gstNumber;
    private String paymentGateway;
    private String paymentId;
    private Subscription.Status status;
    private String downloadUrl;
}
