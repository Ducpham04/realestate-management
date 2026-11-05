package com.realestate.appointment.dto;

import com.realestate.appointment.AppointmentStatus;
import com.realestate.customer.dto.CustomerSummary;
import com.realestate.property.dto.PropertySummary;
import com.realestate.user.dto.UserSummary;
import java.time.Instant;

public record AppointmentResponse(
        Long id,
        PropertySummary property,
        CustomerSummary customer,
        UserSummary agent,
        Instant scheduledAt,
        Instant endAt,
        AppointmentStatus status,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
