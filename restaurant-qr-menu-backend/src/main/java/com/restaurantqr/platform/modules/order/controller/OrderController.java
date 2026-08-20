package com.restaurantqr.platform.modules.order.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restaurants/{restaurantId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN','CHEF')")
    public ResponseEntity<ApiResponse<List<Order>>> getOrders(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.findByRestaurant(restaurantId)));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN','CHEF')")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable Long restaurantId,
            @PathVariable String orderId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        Order.Status status = Order.Status.valueOf(statusStr.toUpperCase());
        Order updated = orderService.updateStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updated));
    }
}
