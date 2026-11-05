package com.realestate.deal.dto;

import com.realestate.deal.DealStatus;
import jakarta.validation.constraints.NotNull;

public record DealStatusRequest(@NotNull DealStatus status) {
}
