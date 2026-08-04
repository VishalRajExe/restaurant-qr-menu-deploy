package com.restaurantqr.platform.modules.subscription.dto;

import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageMeterDto {

    private Long restaurantId;
    private String restaurantName;
    private Restaurant.SubscriptionPlan currentPlan;
    private boolean isTrial;
    private LocalDateTime trialEndsAt;
    private boolean trialExpired;

    // Quotas & Usage
    private long branchesUsed;
    private long branchesLimit; // -1 for unlimited

    private long menuItemsUsed;
    private long menuItemsLimit; // -1 for unlimited

    private long staffUsersUsed;
    private long staffUsersLimit; // -1 for unlimited

    private long storageUsedMb;
    private long storageLimitMb; // -1 for unlimited

    private long totalQrScansThisMonth;
    private long monthlyVisitors;
}
