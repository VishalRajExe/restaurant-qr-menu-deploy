package com.restaurantqr.platform.modules.table.dto;

import com.restaurantqr.platform.modules.table.entity.DiningTable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableRequest {
    @NotBlank(message = "Table number is required")
    private String tableNumber;

    @NotNull(message = "Capacity is required")
    private Integer capacity;

    private Long branchId;

    private DiningTable.Status status;
}
