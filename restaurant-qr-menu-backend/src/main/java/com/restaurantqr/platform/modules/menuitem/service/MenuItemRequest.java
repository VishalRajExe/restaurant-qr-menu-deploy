package com.restaurantqr.platform.modules.menuitem.service;

import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemRequest {

    @NotNull(message = "Category is required")
    public Long categoryId;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    public String name;

    public String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    public BigDecimal price;

    @NotNull(message = "Food type is required")
    public MenuItem.FoodType vegNonveg;

    public Boolean isAvailable;
    public Boolean isFeatured;
    public Boolean isPopular;
    public Boolean isChefSpecial;
    public Integer spiceLevel;
    public Integer calories;
    public BigDecimal proteinGrams;
    public BigDecimal fatGrams;
    public BigDecimal carbsGrams;
    public String allergens;
    public Boolean isVegan;
    public Boolean isHalal;
    public Boolean isJain;
    public Boolean isGlutenFree;
    public MenuItem.MealType mealType;
    public Boolean isCombo;
    public String comboDescription;
    public Integer prepTimeMinutes;
    public Integer displayOrder;
    public String tags;
}

