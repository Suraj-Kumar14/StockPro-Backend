package com.stockpro.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long adminCount;
    private long inventoryManagerCount;
    private long purchaseOfficerCount;
    private long warehouseStaffCount;
    private long recentLoginCount;
}
