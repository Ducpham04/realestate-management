package com.realestate.deal.dto;

import com.realestate.customer.dto.CustomerSummary;
import com.realestate.deal.DealStatus;
import com.realestate.property.dto.PropertySummary;
import com.realestate.user.dto.UserSummary;
import java.math.BigDecimal;
import java.time.Instant;

public record DealResponse(
        Long id,
        PropertySummary property,
        CustomerSummary customer,
        UserSummary agent,
        BigDecimal amount,
        BigDecimal commission,
        DealStatus status,
        Instant closedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
