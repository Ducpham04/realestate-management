package com.realestate.property.dto;

import com.realestate.property.ListingType;
import com.realestate.property.PropertyStatus;
import com.realestate.property.PropertyType;
import java.math.BigDecimal;

public record PropertyFilter(
        String keyword,
        PropertyType type,
        ListingType listingType,
        PropertyStatus status,
        String city,
        String district,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minArea,
        BigDecimal maxArea,
        Integer minBedrooms,
        Long agentId
) {
}
