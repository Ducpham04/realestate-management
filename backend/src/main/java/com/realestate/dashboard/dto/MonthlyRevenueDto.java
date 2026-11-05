package com.realestate.dashboard.dto;

import java.math.BigDecimal;

/** Revenue of closed deals for one calendar month; {@code month} is formatted yyyy-MM. */
public record MonthlyRevenueDto(String month, long deals, BigDecimal revenue, BigDecimal commission) {
}
