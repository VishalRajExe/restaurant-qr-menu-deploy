package com.restaurantqr.platform.analytics.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs",
        indexes = @Index(name = "idx_search_restaurant_term", columnList = "restaurant_id, search_term"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "search_term", nullable = false, length = 150)
    private String searchTerm;

    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Integer searchCount = 1;

    @Column(name = "last_searched_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastSearchedAt = LocalDateTime.now();
}
