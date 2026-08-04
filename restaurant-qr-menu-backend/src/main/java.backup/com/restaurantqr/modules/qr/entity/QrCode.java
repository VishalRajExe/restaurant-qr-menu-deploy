package com.restaurantqr.modules.qr.entity;

import com.restaurantqr.common.BaseEntity;

import com.restaurantqr.modules.branch.entity.Branch;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "qr_codes",
        indexes = {
                @Index(name = "idx_qr_branch", columnList = "branch_id"),
                @Index(name = "idx_qr_token", columnList = "token")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode extends BaseEntity {

	// Exposed so the admin dashboard can display which branch a QR belongs to.
	// Branch.restaurant is itself @JsonIgnore'd, so this can't recurse.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "branch_id", nullable = false)
	private Branch branch;

	// Redundant with the {restaurantId} already in the URL path — keep hidden.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "restaurant_id", nullable = false)
	@JsonIgnore
	private Restaurant restaurant;

    @Column(name = "table_number", length = 20)
    private String tableNumber;   // Optional (null = whole restaurant)

    @Column(name = "label", length = 100)
    private String label;   // e.g. "Table 12", "Rooftop Bar", "Counter"

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;   // UUID-based, used in the URL

    @Column(name = "qr_image_url")
    private String qrImageUrl;   // Cloudinary URL of the generated QR PNG

    @Column(name = "scan_count", nullable = false)
    @Builder.Default
    private Long scanCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public void incrementScan() {
        this.scanCount++;
    }

    public enum Status { ACTIVE, INACTIVE }
}
