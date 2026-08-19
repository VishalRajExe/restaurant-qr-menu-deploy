package com.restaurantqr.platform.modules.order.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.order.entity.OrderItem;
import com.restaurantqr.platform.modules.order.repository.OrderRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantService restaurantService;

    @Transactional
    public Order createOrder(OrderRequest request) {
        Restaurant restaurant;
        if (request.getRestaurantId() != null) {
            restaurant = restaurantService.findById(request.getRestaurantId());
        } else if (request.getRestaurantSlug() != null && !request.getRestaurantSlug().isBlank()) {
            restaurant = restaurantService.findBySlug(request.getRestaurantSlug());
        } else {
            restaurant = restaurantService.findById(1L);
        }

        String orderNumber = "ORD-" + System.currentTimeMillis() % 1000000 + "-" + (int) (Math.random() * 900 + 100);

        Order order = Order.builder()
                .restaurant(restaurant)
                .orderNumber(orderNumber)
                .tableNumber(request.getTableNumber() != null && !request.getTableNumber().isBlank() ? request.getTableNumber() : "01")
                .customerMobile(request.getCustomerMobile())
                .customerName(request.getCustomerName() != null ? request.getCustomerName() : "Customer")
                .specialInstructions(request.getSpecialInstructions())
                .status(Order.Status.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            BigDecimal price = itemReq.getPrice() != null ? itemReq.getPrice() : BigDecimal.ZERO;
            int qty = itemReq.getQuantity() != null && itemReq.getQuantity() > 0 ? itemReq.getQuantity() : 1;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(qty));
            grandTotal = grandTotal.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItemId(itemReq.getMenuItemId())
                    .itemName(itemReq.getItemName())
                    .price(price)
                    .quantity(qty)
                    .subtotal(subtotal)
                    .notes(itemReq.getNotes())
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(grandTotal);
        Order saved = orderRepository.save(order);
        log.info("Created multi-item order: orderNumber={} customerMobile={} total={}", saved.getOrderNumber(), saved.getCustomerMobile(), saved.getTotalAmount());
        return saved;
    }

    public List<Order> findByRestaurant(Long restaurantId) {
        restaurantService.findById(restaurantId);
        return orderRepository.findByRestaurantIdOrdered(restaurantId);
    }

    public Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
    }

    public List<Order> trackOrders(String identifier) {
        return trackOrders(identifier, null);
    }

    public List<Order> trackOrders(String identifier, Long restaurantId) {
        if (restaurantId != null) {
            return orderRepository.findByRestaurantIdAndCustomerMobileOrOrderNumber(restaurantId, identifier);
        }
        return orderRepository.findByCustomerMobileOrOrderNumber(identifier);
    }

    @Transactional
    public Order updateStatus(Long orderId, Order.Status status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        log.info("Updated status for orderId={} to {}", orderId, status);
        return updated;
    }

    @Transactional
    public Order updateStatusByOrderNumber(String orderNumber, Order.Status status) {
        Order order = findByOrderNumber(orderNumber);
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
