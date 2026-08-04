package com.restaurantqr.modules.analytics.entity;

import com.restaurantqr.modules.qr.entity.QrCode;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scan_events",
        indexes = {
                @Index(name = "idx_scan_restaurant", columnList = "restaurant_id"),
                @Index(name = "idx_scan_created_at", columnList = "created_at"),
                @Index(name = "idx_scan_qr", columnList = "qr_code_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qr_code_id", nullable = false)
    private QrCode qrCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type")
    private DeviceType deviceType;

    @Column(name = "country", length = 60)
    private String country;

    public enum DeviceType { MOBILE, TABLET, DESKTOP, UNKNOWN }
}
