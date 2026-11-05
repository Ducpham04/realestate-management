package com.realestate.property.dto;

import com.realestate.property.ListingType;
import com.realestate.property.PropertyStatus;
import com.realestate.property.PropertyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PropertyRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotNull PropertyType type,
        @NotNull ListingType listingType,
        /* Optional on create (defaults to AVAILABLE). */
        PropertyStatus status,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 13, fraction = 2) BigDecimal price,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("99999999") @Digits(integer = 8, fraction = 2)
        BigDecimal area,
        @Min(0) @Max(100) Integer bedrooms,
        @Min(0) @Max(100) Integer bathrooms,
        @NotNull @Valid AddressDto address,
        @Size(max = 20) List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "must be an http(s) URL") String> images,
        /* Responsible agent; only admins may set it, agents are always assigned to themselves. */
        Long agentId
) {
}
