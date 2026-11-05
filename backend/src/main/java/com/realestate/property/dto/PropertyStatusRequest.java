package com.realestate.property.dto;

import com.realestate.property.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusRequest(@NotNull PropertyStatus status) {
}
