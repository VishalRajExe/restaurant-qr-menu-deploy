package com.restaurantqr.platform.modules.table.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSummaryStats {
    private long totalTables;
    private long availableTables;
    private long occupiedTables;
    private long reservedTables;
    private long cleaningTables;
}
