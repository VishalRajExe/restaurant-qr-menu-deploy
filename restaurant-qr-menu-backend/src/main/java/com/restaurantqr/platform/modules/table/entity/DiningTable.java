package com.restaurantqr.platform.modules.table.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "dining_tables", indexes = {
        @Index(name = "idx_dining_table_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_dining_table_branch", columnList = "branch_id"),
        @Index(name = "idx_dining_table_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiningTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "table_number", nullable = false, length = 50)
    private String tableNumber; // e.g. "Table 01", "01", "T-01"

    @Column(name = "capacity", nullable = false)
    @Builder.Default
    private Integer capacity = 4;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.AVAILABLE;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "qr_code_id")
    private QrCode qrCode;

    // Reservation details (when status is RESERVED)
    @Column(name = "reservation_name", length = 100)
    private String reservationName;

    @Column(name = "reservation_phone", length = 30)
    private String reservationPhone;

    @Column(name = "reservation_time", length = 50)
    private String reservationTime;

    @Column(name = "reservation_guests")
    private Integer reservationGuests;

    @Column(name = "reservation_notes", length = 500)
    private String reservationNotes;

    // Session Tracking (when OCCUPIED)
    @Column(name = "active_session_id", length = 64)
    private String activeSessionId;

    @Column(name = "session_start_time")
    private LocalDateTime sessionStartTime;

    public enum Status {
        AVAILABLE,
        RESERVED,
        OCCUPIED,
        CLEANING
    }
}
