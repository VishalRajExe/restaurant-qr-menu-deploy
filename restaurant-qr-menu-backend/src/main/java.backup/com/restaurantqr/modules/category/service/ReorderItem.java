package com.restaurantqr.modules.category.service;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReorderItem {
    @NotNull
    public Long id;
    @NotNull
    public Integer displayOrder;
}
