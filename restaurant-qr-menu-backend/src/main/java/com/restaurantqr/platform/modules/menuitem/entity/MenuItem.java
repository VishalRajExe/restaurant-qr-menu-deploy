package com.restaurantqr.platform.modules.menuitem.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
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
    @com.fasterxml.jackson.annotation.JsonIgnore
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

    @Column(name = "is_popular", nullable = false)
    @Builder.Default
    private Boolean isPopular = false;

    @Column(name = "is_chef_special", nullable = false)
    @Builder.Default
    private Boolean isChefSpecial = false;

    @Column(name = "spice_level")
    @Builder.Default
    private Integer spiceLevel = 0;

    @Column(name = "calories")
    private Integer calories;

    @Column(name = "protein_grams", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal proteinGrams = BigDecimal.ZERO;

    @Column(name = "fat_grams", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal fatGrams = BigDecimal.ZERO;

    @Column(name = "carbs_grams", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal carbsGrams = BigDecimal.ZERO;

    @Column(name = "allergens")
    private String allergens;

    @Column(name = "is_vegan", nullable = false)
    @Builder.Default
    private Boolean isVegan = false;

    @Column(name = "is_halal", nullable = false)
    @Builder.Default
    private Boolean isHalal = false;

    @Column(name = "is_jain", nullable = false)
    @Builder.Default
    private Boolean isJain = false;

    @Column(name = "is_gluten_free", nullable = false)
    @Builder.Default
    private Boolean isGlutenFree = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    @Builder.Default
    private MealType mealType = MealType.ALL_DAY;

    @Column(name = "is_combo", nullable = false)
    @Builder.Default
    private Boolean isCombo = false;

    @Column(name = "combo_description", columnDefinition = "TEXT")
    private String comboDescription;

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
    public enum MealType { BREAKFAST, LUNCH, DINNER, ALL_DAY }
    public enum Status { ACTIVE, INACTIVE }
}

