package com.realestate.deal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DealRequest(
        @NotNull Long propertyId,
        @NotNull Long customerId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull @DecimalMin("0.0") @Digits(integer = 13, fraction = 2) BigDecimal commission,
        @Size(max = 5000) String notes,
        /* Only honoured for admins. */
        Long agentId
) {

    @AssertTrue(message = "commission must not exceed amount")
    public boolean isCommissionValid() {
        return amount == null || commission == null || commission.compareTo(amount) <= 0;
    }
}
