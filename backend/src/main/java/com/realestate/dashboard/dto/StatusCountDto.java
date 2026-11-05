package com.realestate.dashboard.dto;

import com.realestate.property.PropertyStatus;

public record StatusCountDto(PropertyStatus status, long count) {
}
