package com.restaurantqr.platform.modules.table.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.table.dto.*;
import com.restaurantqr.platform.modules.table.entity.DiningTable;
import com.restaurantqr.platform.modules.table.service.DiningTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restaurants/{restaurantId}/tables")
@RequiredArgsConstructor
public class DiningTableController {

    private final DiningTableService tableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<List<TableDto>>> getTables(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) Long branchId) {
        List<TableDto> tables = tableService.getTables(restaurantId, branchId);
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<TableSummaryStats>> getStats(@PathVariable Long restaurantId) {
        TableSummaryStats stats = tableService.getStats(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/{tableId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<TableDto>> getTableDetails(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId) {
        TableDto details = tableService.getTableDetails(restaurantId, tableId);
        return ResponseEntity.ok(ApiResponse.success(details));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<TableDto>> createTable(
            @PathVariable Long restaurantId,
            @Valid @RequestBody TableRequest request) {
        TableDto created = tableService.createTable(restaurantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Dining table and QR code generated", created));
    }

    @PutMapping("/{tableId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<TableDto>> updateTable(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId,
            @Valid @RequestBody TableRequest request) {
        TableDto updated = tableService.updateTable(restaurantId, tableId, request);
        return ResponseEntity.ok(ApiResponse.success("Dining table updated", updated));
    }

    @DeleteMapping("/{tableId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteTable(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId) {
        tableService.deleteTable(restaurantId, tableId);
        return ResponseEntity.ok(ApiResponse.success("Dining table deleted", null));
    }

    @PatchMapping("/{tableId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<TableDto>> updateStatus(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        DiningTable.Status status = DiningTable.Status.valueOf(statusStr.toUpperCase());
        TableDto updated = tableService.updateStatus(restaurantId, tableId, status);
        return ResponseEntity.ok(ApiResponse.success("Table status updated", updated));
    }

    @PostMapping("/{tableId}/reserve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<TableDto>> reserveTable(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId,
            @Valid @RequestBody ReservationRequest request) {
        TableDto reserved = tableService.reserveTable(restaurantId, tableId, request);
        return ResponseEntity.ok(ApiResponse.success("Table reservation confirmed", reserved));
    }

    @PostMapping("/{tableId}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<TableDto>> closeTable(
            @PathVariable Long restaurantId,
            @PathVariable Long tableId) {
        TableDto closed = tableService.closeTable(restaurantId, tableId);
        return ResponseEntity.ok(ApiResponse.success("Table closed and marked for cleaning", closed));
    }
}
