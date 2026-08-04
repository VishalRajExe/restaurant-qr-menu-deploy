package com.restaurantqr.modules.menuitem.entity;

import com.restaurantqr.common.BaseEntity;
import com.restaurantqr.modules.category.entity.Category;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "menu_items",
        indexes = {
                @Index(name = "idx_menuitem_category", columnList = "category_id"),
                @Index(name = "idx_menuitem_restaurant", columnList = "restaurant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Denormalized for efficient filtering
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "veg_nonveg", nullable = false)
    @Builder.Default
    private FoodType vegNonveg = FoodType.NON_VEG;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "prep_time_minutes")
    private Integer prepTimeMinutes;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "tags", length = 300)
    private String tags;   // Comma-separated: "spicy,chef-special"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public enum FoodType { VEG, NON_VEG, EGG, VEGAN }
    public enum Status { ACTIVE, INACTIVE }
}
