package com.restaurantqr.platform.modules.customer.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.customer.dto.CustomerHistoryDto;
import com.restaurantqr.platform.modules.customer.dto.CustomerSummaryDto;
import com.restaurantqr.platform.modules.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<CustomerHistoryDto>> getCustomerOrderHistory(
            @PathVariable Long restaurantId,
            @RequestParam String phone) {
        CustomerHistoryDto history = customerService.getCustomerOrderHistory(restaurantId, phone);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<CustomerSummaryDto>>> getRecentCustomers(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "100") int limit) {
        List<CustomerSummaryDto> customers = customerService.getRecentCustomers(restaurantId, search, limit);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }
}
