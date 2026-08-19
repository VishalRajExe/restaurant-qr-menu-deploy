package com.restaurantqr.platform.modules.order.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.order.service.OrderRequest;
import com.restaurantqr.platform.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/orders")
@RequiredArgsConstructor
public class PublicOrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping("/track")
    public ResponseEntity<ApiResponse<List<Order>>> trackOrders(
            @RequestParam String identifier,
            @RequestParam(required = false) Long restaurantId) {
        List<Order> orders = orderService.trackOrders(identifier, restaurantId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<Order>> getOrderByNumber(@PathVariable String orderNumber) {
        Order order = orderService.findByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
