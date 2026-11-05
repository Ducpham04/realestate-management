package com.realestate.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummary(
        long totalProperties,
        long availableProperties,
        long totalCustomers,
        long upcomingAppointments,
        long activeDeals,
        long closedDealsThisMonth,
        BigDecimal revenueThisMonth,
        BigDecimal commissionThisMonth
) {
}
