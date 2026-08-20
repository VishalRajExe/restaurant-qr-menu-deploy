package com.restaurantqr.platform.modules.customer.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSummaryDto {

    private String customerMobile;
    private String customerName;
    private int orderCount;
    private BigDecimal totalSpent;
    private LocalDateTime lastOrderDate;
}
