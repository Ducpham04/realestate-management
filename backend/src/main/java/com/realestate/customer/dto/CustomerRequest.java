package com.realestate.customer.dto;

import com.realestate.customer.CustomerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CustomerRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$", message = "is not a valid phone number") String phone,
        @NotNull CustomerType type,
        @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal budgetMin,
        @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal budgetMax,
        @Size(max = 200) String preferredLocation,
        @Size(max = 5000) String notes,
        Long agentId
) {

    @AssertTrue(message = "budgetMin must not be greater than budgetMax")
    public boolean isBudgetRangeValid() {
        return budgetMin == null || budgetMax == null || budgetMin.compareTo(budgetMax) <= 0;
    }
}
