package com.realestate.customer.dto;

import com.realestate.customer.CustomerType;
import com.realestate.user.dto.UserSummary;
import java.math.BigDecimal;
import java.time.Instant;

public record CustomerResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        CustomerType type,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String preferredLocation,
        String notes,
        UserSummary agent,
        Instant createdAt,
        Instant updatedAt
) {
}
