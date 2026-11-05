package com.realestate.property.dto;

import com.realestate.property.ListingType;
import com.realestate.property.PropertyStatus;
import com.realestate.property.PropertyType;
import com.realestate.user.dto.UserSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PropertyResponse(
        Long id,
        String title,
        String description,
        PropertyType type,
        ListingType listingType,
        PropertyStatus status,
        BigDecimal price,
        BigDecimal area,
        Integer bedrooms,
        Integer bathrooms,
        AddressDto address,
        List<String> images,
        UserSummary agent,
        Instant createdAt,
        Instant updatedAt
) {
}
