package com.restaurantqr.platform.modules.table.dto;

import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.table.entity.DiningTable;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableDto {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private Long branchId;
    private String branchName;
    private String tableNumber;
    private Integer capacity;
    private DiningTable.Status status;
    
    // QR Code info
    private Long qrCodeId;
    private String qrToken;
    private String qrImageUrl;
    private Long scanCount;

    // Reservation Details
    private String reservationName;
    private String reservationPhone;
    private String reservationTime;
    private Integer reservationGuests;
    private String reservationNotes;

    // Live Session & Orders
    private String activeSessionId;
    private LocalDateTime sessionStartTime;
    private Integer activeOrdersCount;
    private BigDecimal currentTotalAmount;
    private List<Order> activeOrders;
}
