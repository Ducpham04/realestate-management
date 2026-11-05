package com.realestate.appointment.dto;

import com.realestate.appointment.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status, @Size(max = 1000) String note) {
}
