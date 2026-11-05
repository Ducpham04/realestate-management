package com.realestate.appointment;

import com.realestate.appointment.dto.AppointmentRequest;
import com.realestate.appointment.dto.AppointmentResponse;
import com.realestate.appointment.dto.AppointmentStatusRequest;
import com.realestate.common.api.ApiResponse;
import com.realestate.common.api.PageResponse;
import com.realestate.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Property viewing schedule")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Search appointments", description = "Agents only see their own appointments. "
            + "from/to are ISO-8601 instants, e.g. 2026-01-01T00:00:00Z.")
    public ApiResponse<PageResponse<AppointmentResponse>> search(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(appointmentService.search(status, propertyId, customerId, agentId, from, to,
                pageable, actor));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(appointmentService.getById(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request,
                                                   @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Appointment scheduled", appointmentService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody AppointmentRequest request,
                                                   @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Appointment updated", appointmentService.update(id, request, actor));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AppointmentResponse> changeStatus(@PathVariable Long id,
                                                         @Valid @RequestBody AppointmentStatusRequest request,
                                                         @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Appointment status updated",
                appointmentService.changeStatus(id, request.status(), request.note(), actor));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        appointmentService.delete(id, actor);
        return ApiResponse.ok("Appointment deleted", null);
    }
}
