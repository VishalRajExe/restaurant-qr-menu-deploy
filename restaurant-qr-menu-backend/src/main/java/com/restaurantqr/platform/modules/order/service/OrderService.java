package com.restaurantqr.platform.modules.order.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.service.NotificationService;
import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.order.entity.OrderItem;
import com.restaurantqr.platform.modules.order.repository.OrderRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.table.service.DiningTableService;
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
    private final NotificationService notificationService;
    private final DiningTableService diningTableService;

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

        try {
            if (saved.getRestaurant() != null) {
                notificationService.notifyRestaurant(
                        saved.getRestaurant().getId(),
                        Notification.EventType.NEW_ORDER,
                        "New Kitchen Order Placed",
                        "New order #" + saved.getOrderNumber() + " received for Table " + saved.getTableNumber()
                );
                // Automatically link and transition Table status from Available/Reserved to OCCUPIED
                diningTableService.markTableOccupiedForOrder(
                        saved.getRestaurant().getId(),
                        saved.getTableNumber(),
                        saved.getCustomerName(),
                        saved.getCustomerMobile()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send new order notification or update table status: {}", e.getMessage());
        }

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
        return updateStatus(String.valueOf(orderId), status);
    }

    @Transactional
    public Order updateStatus(String orderIdentifier, Order.Status status) {
        if (orderIdentifier == null || orderIdentifier.isBlank()) {
            throw new ResourceNotFoundException("Invalid order identifier");
        }

        Order order = null;
        try {
            Long numericId = Long.parseLong(orderIdentifier.trim());
            order = orderRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (order == null) {
            order = orderRepository.findByOrderNumber(orderIdentifier.trim()).orElse(null);
        }

        if (order == null && !orderIdentifier.startsWith("ORD-")) {
            order = orderRepository.findByOrderNumber("ORD-" + orderIdentifier.trim()).orElse(null);
        }

        if (order == null) {
            List<Order> matches = orderRepository.findByOrderNumberContaining(orderIdentifier.trim());
            if (!matches.isEmpty()) {
                order = matches.get(0);
            }
        }

        if (order == null) {
            throw new ResourceNotFoundException("Order not found with identifier: " + orderIdentifier);
        }

        Order.Status prevStatus = order.getStatus();
        order.setStatus(status);
        Order updated = orderRepository.save(order);
        log.info("Updated status for order id={} number={} to {}", updated.getId(), updated.getOrderNumber(), status);

        try {
            if (updated.getRestaurant() != null && prevStatus != status) {
                if (status == Order.Status.READY) {
                    notificationService.notifyRestaurant(
                            updated.getRestaurant().getId(),
                            Notification.EventType.ORDER_READY,
                            "Kitchen Alert: Order Ready",
                            "Chef finished preparing Order #" + updated.getOrderNumber() + " for Table " + updated.getTableNumber() + ". Ready to be served."
                    );
                } else if (status == Order.Status.COMPLETED || status == Order.Status.DELIVERED) {
                    notificationService.notifyRestaurant(
                            updated.getRestaurant().getId(),
                            Notification.EventType.ORDER_STATUS_CHANGED,
                            "Order Delivered",
                            "Order #" + updated.getOrderNumber() + " (Table " + updated.getTableNumber() + ") marked as delivered to guest."
                    );
                } else if (status == Order.Status.PREPARING) {
                    notificationService.notifyRestaurant(
                            updated.getRestaurant().getId(),
                            Notification.EventType.ORDER_STATUS_CHANGED,
                            "Cooking in Kitchen",
                            "Chef started preparing Order #" + updated.getOrderNumber() + " (Table " + updated.getTableNumber() + ")."
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch order status notification: {}", e.getMessage());
        }

        return updated;
    }

    @Transactional
    public Order updateStatusByOrderNumber(String orderNumber, Order.Status status) {
        return updateStatus(orderNumber, status);
    }
}
