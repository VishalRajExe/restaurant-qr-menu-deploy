package com.restaurantqr.platform.modules.customer.dto;

import com.restaurantqr.platform.modules.order.entity.Order;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerHistoryDto {

    private String customerMobile;
    private String customerName;
    private Long restaurantId;
    private String restaurantName;
    private int totalOrders;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private LocalDateTime firstOrderDate;
    private LocalDateTime lastOrderDate;
    private List<FavoriteItemDto> favoriteItems;
    private List<Order> orders;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FavoriteItemDto {
        private String itemName;
        private int totalQuantity;
        private BigDecimal totalAmount;
    }
}
