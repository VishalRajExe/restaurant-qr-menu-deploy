package com.restaurantqr.platform.modules.order.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {

    private Long restaurantId;

    private String restaurantSlug;

    private String tableNumber;

    @NotBlank(message = "Customer mobile number is required")
    private String customerMobile;

    private String customerName;

    private String specialInstructions;

    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Long menuItemId;

        @NotBlank(message = "Item name is required")
        private String itemName;

        @NotNull(message = "Price is required")
        private BigDecimal price;

        @NotNull(message = "Quantity is required")
        private Integer quantity;

        private String notes;
    }
}
