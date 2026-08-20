package com.restaurantqr.platform.modules.table.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.service.NotificationService;
import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.order.repository.OrderRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.table.dto.*;
import com.restaurantqr.platform.modules.table.entity.DiningTable;
import com.restaurantqr.platform.modules.table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository tableRepository;
    private final RestaurantService restaurantService;
    private final BranchRepository branchRepository;
    private final QrCodeRepository qrCodeRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public List<TableDto> getTables(Long restaurantId, Long branchId) {
        restaurantService.findById(restaurantId);
        List<DiningTable> tables;
        if (branchId != null) {
            tables = tableRepository.findByRestaurantIdAndBranchIdOrderByTableNumberAsc(restaurantId, branchId);
        } else {
            tables = tableRepository.findByRestaurantIdOrderByTableNumberAsc(restaurantId);
        }

        // Fetch active orders for this restaurant to aggregate live session stats per table
        List<Order> restaurantOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);

        return tables.stream()
                .map(table -> mapToDto(table, restaurantOrders))
                .collect(Collectors.toList());
    }

    public TableSummaryStats getStats(Long restaurantId) {
        restaurantService.findById(restaurantId);
        long total = tableRepository.countByRestaurantId(restaurantId);
        long available = tableRepository.countByRestaurantIdAndStatus(restaurantId, DiningTable.Status.AVAILABLE);
        long occupied = tableRepository.countByRestaurantIdAndStatus(restaurantId, DiningTable.Status.OCCUPIED);
        long reserved = tableRepository.countByRestaurantIdAndStatus(restaurantId, DiningTable.Status.RESERVED);
        long cleaning = tableRepository.countByRestaurantIdAndStatus(restaurantId, DiningTable.Status.CLEANING);

        return TableSummaryStats.builder()
                .totalTables(total)
                .availableTables(available)
                .occupiedTables(occupied)
                .reservedTables(reserved)
                .cleaningTables(cleaning)
                .build();
    }

    public TableDto getTableDetails(Long restaurantId, Long tableId) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Table does not belong to restaurant: " + restaurantId);
        }

        List<Order> restaurantOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);
        return mapToDto(table, restaurantOrders);
    }

    @Transactional
    public TableDto createTable(Long restaurantId, TableRequest request) {
        Restaurant restaurant = restaurantService.findById(restaurantId);

        Branch branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId()).orElse(null);
        }
        if (branch == null) {
            List<Branch> branches = branchRepository.findByRestaurantId(restaurantId);
            if (!branches.isEmpty()) {
                branch = branches.get(0);
            }
        }

        String rawNum = request.getTableNumber().trim();
        String formattedNumber = rawNum.matches("^\\d+$")
                ? "Table " + (Integer.parseInt(rawNum) < 10 ? "0" + Integer.parseInt(rawNum) : rawNum)
                : (rawNum.toLowerCase().startsWith("table") ? rawNum : "Table " + rawNum);

        // Check for existing table
        Optional<DiningTable> existing = tableRepository.findByRestaurantIdAndTableNumber(restaurantId, formattedNumber);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A table with number '" + formattedNumber + "' already exists in this restaurant.");
        }

        // Generate unique QR Code token
        String qrToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String tableParam = formattedNumber.replaceFirst("(?i)^Table\\s*", "");
        String publicMenuUrl = "http://localhost:4200/menu/" + (restaurant.getSlug() != null ? restaurant.getSlug() : restaurantId) + "?table=" + URLEncoder.encode(tableParam, StandardCharsets.UTF_8);
        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&ecc=H&color=101828&data=" + URLEncoder.encode(publicMenuUrl, StandardCharsets.UTF_8);

        QrCode qrCode = QrCode.builder()
                .restaurant(restaurant)
                .branch(branch)
                .tableNumber(formattedNumber)
                .label(formattedNumber)
                .token(qrToken)
                .qrImageUrl(qrImageUrl)
                .scanCount(0L)
                .status(QrCode.Status.ACTIVE)
                .build();
        qrCode = qrCodeRepository.save(qrCode);

        DiningTable table = DiningTable.builder()
                .restaurant(restaurant)
                .branch(branch)
                .tableNumber(formattedNumber)
                .capacity(request.getCapacity() != null ? request.getCapacity() : 4)
                .status(request.getStatus() != null ? request.getStatus() : DiningTable.Status.AVAILABLE)
                .qrCode(qrCode)
                .build();

        DiningTable saved = tableRepository.save(table);
        log.info("Created dining table id={} number={} for restaurantId={}", saved.getId(), saved.getTableNumber(), restaurantId);

        return mapToDto(saved, Collections.emptyList());
    }

    @Transactional
    public TableDto updateTable(Long restaurantId, Long tableId, TableRequest request) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Table does not belong to restaurant: " + restaurantId);
        }

        if (request.getCapacity() != null) {
            table.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null) {
            table.setStatus(request.getStatus());
        }
        if (request.getBranchId() != null) {
            branchRepository.findById(request.getBranchId()).ifPresent(table::setBranch);
        }

        if (request.getTableNumber() != null && !request.getTableNumber().isBlank()) {
            String rawNum = request.getTableNumber().trim();
            String formattedNumber = rawNum.matches("^\\d+$")
                    ? "Table " + (Integer.parseInt(rawNum) < 10 ? "0" + Integer.parseInt(rawNum) : rawNum)
                    : (rawNum.toLowerCase().startsWith("table") ? rawNum : "Table " + rawNum);
            table.setTableNumber(formattedNumber);

            if (table.getQrCode() != null) {
                table.getQrCode().setTableNumber(formattedNumber);
                table.getQrCode().setLabel(formattedNumber);
                qrCodeRepository.save(table.getQrCode());
            }
        }

        DiningTable updated = tableRepository.save(table);
        List<Order> restaurantOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);
        return mapToDto(updated, restaurantOrders);
    }

    @Transactional
    public void deleteTable(Long restaurantId, Long tableId) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Table does not belong to restaurant: " + restaurantId);
        }

        tableRepository.delete(table);
        log.info("Deleted dining table id={} for restaurantId={}", tableId, restaurantId);
    }

    @Transactional
    public TableDto updateStatus(Long restaurantId, Long tableId, DiningTable.Status status) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Table does not belong to restaurant: " + restaurantId);
        }

        DiningTable.Status prevStatus = table.getStatus();
        table.setStatus(status);

        if (status == DiningTable.Status.AVAILABLE) {
            table.setReservationName(null);
            table.setReservationPhone(null);
            table.setReservationTime(null);
            table.setReservationGuests(null);
            table.setReservationNotes(null);
            table.setActiveSessionId(null);
            table.setSessionStartTime(null);
        } else if (status == DiningTable.Status.OCCUPIED) {
            if (table.getSessionStartTime() == null) {
                table.setSessionStartTime(LocalDateTime.now());
                table.setActiveSessionId("SES-" + System.currentTimeMillis());
            }
        }

        DiningTable saved = tableRepository.save(table);
        log.info("Updated status of table id={} to {}", tableId, status);

        try {
            if (prevStatus != status) {
                notificationService.notifyRestaurant(
                        restaurantId,
                        Notification.EventType.SYSTEM_NOTICE,
                        "Table Status Updated",
                        table.getTableNumber() + " status changed from " + prevStatus + " to " + status + "."
                );
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch table status notification: {}", e.getMessage());
        }

        List<Order> restaurantOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);
        return mapToDto(saved, restaurantOrders);
    }

    @Transactional
    public TableDto reserveTable(Long restaurantId, Long tableId, ReservationRequest request) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Table does not belong to restaurant: " + restaurantId);
        }

        table.setStatus(DiningTable.Status.RESERVED);
        table.setReservationName(request.getGuestName());
        table.setReservationPhone(request.getGuestPhone());
        table.setReservationTime(request.getReservationTime());
        table.setReservationGuests(request.getGuestCount() != null ? request.getGuestCount() : table.getCapacity());
        table.setReservationNotes(request.getNotes());

        DiningTable saved = tableRepository.save(table);
        log.info("Reserved table id={} for guest={}", tableId, request.getGuestName());

        try {
            notificationService.notifyRestaurant(
                    restaurantId,
                    Notification.EventType.SYSTEM_NOTICE,
                    "New Table Reservation",
                    table.getTableNumber() + " reserved for " + request.getGuestName() + " (" + request.getReservationTime() + ", " + request.getGuestCount() + " guests)."
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch reservation notification: {}", e.getMessage());
        }

        List<Order> restaurantOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);
        return mapToDto(saved, restaurantOrders);
    }

    @Transactional
    public TableDto closeTable(Long restaurantId, Long tableId) {
        DiningTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + tableId));

        if (!table.getRestaurant().getId().equals(restaurantId)) {
            throw new ResourceNotFoundException("Table does not belong to restaurant: " + restaurantId);
        }

        table.setStatus(DiningTable.Status.CLEANING);
        table.setActiveSessionId(null);
        table.setSessionStartTime(null);
        table.setReservationName(null);
        table.setReservationPhone(null);
        table.setReservationTime(null);
        table.setReservationGuests(null);
        table.setReservationNotes(null);

        DiningTable saved = tableRepository.save(table);
        log.info("Closed table id={} -> CLEANING", tableId);

        try {
            notificationService.notifyRestaurant(
                    restaurantId,
                    Notification.EventType.SYSTEM_NOTICE,
                    "Table Closed for Cleaning",
                    table.getTableNumber() + " marked for cleaning. Prepare for next guest."
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch close table notification: {}", e.getMessage());
        }

        List<Order> restaurantOrders = orderRepository.findByRestaurantIdOrdered(restaurantId);
        return mapToDto(saved, restaurantOrders);
    }

    @Transactional
    public void markTableOccupiedForOrder(Long restaurantId, String rawTableNumber, String customerName, String customerMobile) {
        if (rawTableNumber == null || rawTableNumber.isBlank()) return;

        String cleanNum = rawTableNumber.trim();
        String altNum = cleanNum.matches("^\\d+$") ? "Table " + (Integer.parseInt(cleanNum) < 10 ? "0" + Integer.parseInt(cleanNum) : cleanNum) : cleanNum.replaceFirst("(?i)^Table\\s*", "");

        List<DiningTable> matches = tableRepository.findByRestaurantIdAndTableNumberFuzzy(restaurantId, cleanNum, altNum);
        if (!matches.isEmpty()) {
            DiningTable table = matches.get(0);
            if (table.getStatus() == DiningTable.Status.AVAILABLE || table.getStatus() == DiningTable.Status.RESERVED) {
                table.setStatus(DiningTable.Status.OCCUPIED);
                table.setSessionStartTime(LocalDateTime.now());
                table.setActiveSessionId("SES-" + System.currentTimeMillis());
                tableRepository.save(table);
                log.info("Auto-marked table id={} number={} as OCCUPIED due to new order", table.getId(), table.getTableNumber());
            }
        }
    }

    private TableDto mapToDto(DiningTable table, List<Order> allRestaurantOrders) {
        String num = table.getTableNumber();
        String digitsOnly = num.replaceAll("\\D+", "");

        // Find active orders for this specific table (status in PENDING, ACCEPTED, PREPARING, READY)
        List<Order> activeOrders = allRestaurantOrders.stream()
                .filter(o -> {
                    if (o.getTableNumber() == null) return false;
                    String orderTableDigits = o.getTableNumber().replaceAll("\\D+", "");
                    boolean matches = o.getTableNumber().equalsIgnoreCase(num) ||
                            (digitsOnly.length() > 0 && orderTableDigits.equals(digitsOnly));
                    boolean isActive = o.getStatus() == Order.Status.PENDING ||
                            o.getStatus() == Order.Status.ACCEPTED ||
                            o.getStatus() == Order.Status.PREPARING ||
                            o.getStatus() == Order.Status.READY;
                    return matches && isActive;
                })
                .collect(Collectors.toList());

        BigDecimal totalAmount = activeOrders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TableDto.builder()
                .id(table.getId())
                .restaurantId(table.getRestaurant().getId())
                .restaurantName(table.getRestaurant().getName())
                .branchId(table.getBranch() != null ? table.getBranch().getId() : null)
                .branchName(table.getBranch() != null ? table.getBranch().getName() : "Main Dining Hall")
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .status(table.getStatus())
                .qrCodeId(table.getQrCode() != null ? table.getQrCode().getId() : null)
                .qrToken(table.getQrCode() != null ? table.getQrCode().getToken() : null)
                .qrImageUrl(table.getQrCode() != null ? table.getQrCode().getQrImageUrl() : null)
                .scanCount(table.getQrCode() != null ? table.getQrCode().getScanCount() : 0L)
                .reservationName(table.getReservationName())
                .reservationPhone(table.getReservationPhone())
                .reservationTime(table.getReservationTime())
                .reservationGuests(table.getReservationGuests())
                .reservationNotes(table.getReservationNotes())
                .activeSessionId(table.getActiveSessionId())
                .sessionStartTime(table.getSessionStartTime())
                .activeOrdersCount(activeOrders.size())
                .currentTotalAmount(totalAmount)
                .activeOrders(activeOrders)
                .build();
    }
}
