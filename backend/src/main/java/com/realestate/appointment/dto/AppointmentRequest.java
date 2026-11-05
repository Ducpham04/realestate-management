package com.realestate.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AppointmentRequest(
        @NotNull Long propertyId,
        @NotNull Long customerId,
        @NotNull @Future Instant scheduledAt,
        /* Defaults to 60 minutes. */
        @Min(15) @Max(480) Integer durationMinutes,
        @Size(max = 1000) String note,
        /* Only honoured for admins; agents always own the appointments they create. */
        Long agentId
) {
}
