package com.restaurantqr.platform.modules.customer.service;

import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.customer.dto.CustomerHistoryDto;
import com.restaurantqr.platform.modules.customer.dto.CustomerSummaryDto;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final OrderRepository orderRepository;
    private final RestaurantService restaurantService;

    public String validateAndNormalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new BadRequestException("Phone number is required");
        }
        String clean = rawPhone.replaceAll("\\D", "");
        if (clean.length() != 10) {
            throw new BadRequestException("Phone number must be exactly 10 digits (received: " + clean.length() + " digits)");
        }
        return clean;
    }

    @Transactional(readOnly = true)
    public CustomerHistoryDto getCustomerOrderHistory(Long restaurantId, String rawPhone) {
        String cleanPhone = validateAndNormalizePhone(rawPhone);
        Restaurant restaurant = restaurantService.findById(restaurantId);

        List<Order> allOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);
        List<Order> customerOrders = allOrders.stream()
                .filter(o -> o.getCustomerMobile() != null && o.getCustomerMobile().replaceAll("\\D", "").equals(cleanPhone))
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();

        if (customerOrders.isEmpty()) {
            return CustomerHistoryDto.builder()
                    .customerMobile(cleanPhone)
                    .customerName("New Guest")
                    .restaurantId(restaurantId)
                    .restaurantName(restaurant.getName())
                    .totalOrders(0)
                    .totalSpent(BigDecimal.ZERO)
                    .averageOrderValue(BigDecimal.ZERO)
                    .firstOrderDate(null)
                    .lastOrderDate(null)
                    .favoriteItems(Collections.emptyList())
                    .orders(Collections.emptyList())
                    .build();
        }

        // Determine customer name
        String customerName = customerOrders.stream()
                .map(Order::getCustomerName)
                .filter(n -> n != null && !n.isBlank() && !"Customer".equalsIgnoreCase(n) && !"Guest".equalsIgnoreCase(n))
                .findFirst()
                .orElse("Guest Customer (" + cleanPhone.substring(6) + ")");

        BigDecimal totalSpent = customerOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalOrders = customerOrders.size();
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDateTime lastOrderDate = customerOrders.get(0).getCreatedAt();
        LocalDateTime firstOrderDate = customerOrders.get(customerOrders.size() - 1).getCreatedAt();

        // Calculate favorite items
        Map<String, CustomerHistoryDto.FavoriteItemDto> itemMap = new HashMap<>();
        for (Order order : customerOrders) {
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    String name = item.getItemName() != null ? item.getItemName() : "Menu Item";
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(qty));

                    CustomerHistoryDto.FavoriteItemDto fav = itemMap.getOrDefault(name,
                            CustomerHistoryDto.FavoriteItemDto.builder()
                                    .itemName(name)
                                    .totalQuantity(0)
                                    .totalAmount(BigDecimal.ZERO)
                                    .build());

                    fav.setTotalQuantity(fav.getTotalQuantity() + qty);
                    fav.setTotalAmount(fav.getTotalAmount().add(itemTotal));
                    itemMap.put(name, fav);
                }
            }
        }

        List<CustomerHistoryDto.FavoriteItemDto> favoriteItems = itemMap.values().stream()
                .sorted(Comparator.comparingInt(CustomerHistoryDto.FavoriteItemDto::getTotalQuantity).reversed())
                .limit(5)
                .toList();

        return CustomerHistoryDto.builder()
                .customerMobile(cleanPhone)
                .customerName(customerName)
                .restaurantId(restaurantId)
                .restaurantName(restaurant.getName())
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .averageOrderValue(avgOrderValue)
                .firstOrderDate(firstOrderDate)
                .lastOrderDate(lastOrderDate)
                .favoriteItems(favoriteItems)
                .orders(customerOrders)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerSummaryDto> getRecentCustomers(Long restaurantId, String search, int limit) {
        restaurantService.findById(restaurantId);
        List<Order> orders = orderRepository.findByRestaurantIdOrdered(restaurantId);

        Map<String, List<Order>> grouped = orders.stream()
                .filter(o -> o.getCustomerMobile() != null && !o.getCustomerMobile().isBlank())
                .collect(Collectors.groupingBy(o -> o.getCustomerMobile().replaceAll("\\D", "")));

        List<CustomerSummaryDto> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            String mobile = entry.getKey();
            if (mobile.length() != 10) continue;
            List<Order> custOrders = entry.getValue();

            String name = custOrders.stream()
                    .map(Order::getCustomerName)
                    .filter(n -> n != null && !n.isBlank() && !"Customer".equalsIgnoreCase(n))
                    .findFirst()
                    .orElse("Guest (" + mobile.substring(mobile.length() - 4) + ")");

            if (search != null && !search.isBlank()) {
                String q = search.trim().toLowerCase();
                if (!mobile.contains(q) && !name.toLowerCase().contains(q)) {
                    continue;
                }
            }

            BigDecimal totalSpent = custOrders.stream()
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LocalDateTime lastOrder = custOrders.stream()
                    .map(Order::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            summaries.add(CustomerSummaryDto.builder()
                    .customerMobile(mobile)
                    .customerName(name)
                    .orderCount(custOrders.size())
                    .totalSpent(totalSpent)
                    .lastOrderDate(lastOrder)
                    .build());
        }

        return summaries.stream()
                .sorted(Comparator.comparing(CustomerSummaryDto::getLastOrderDate).reversed())
                .limit(limit > 0 ? limit : 20)
                .toList();
    }
}
