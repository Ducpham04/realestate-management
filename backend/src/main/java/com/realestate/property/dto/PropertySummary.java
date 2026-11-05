package com.realestate.property.dto;

import com.realestate.property.PropertyStatus;
import java.math.BigDecimal;

public record PropertySummary(Long id, String title, PropertyStatus status, BigDecimal price) {
}
