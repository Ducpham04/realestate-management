package com.realestate.appointment;

import com.realestate.appointment.dto.AppointmentRequest;
import com.realestate.appointment.dto.AppointmentResponse;
import com.realestate.common.api.PageResponse;
import com.realestate.common.api.SortSanitizer;
import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.customer.Customer;
import com.realestate.customer.CustomerService;
import com.realestate.property.Property;
import com.realestate.property.PropertyService;
import com.realestate.property.PropertyStatus;
import com.realestate.security.UserPrincipal;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    static final int DEFAULT_DURATION_MINUTES = 60;
    private static final Set<String> SORTABLE = Set.of("id", "scheduledAt", "status", "createdAt");
    private static final Set<PropertyStatus> VIEWABLE = EnumSet.of(PropertyStatus.AVAILABLE, PropertyStatus.RESERVED);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final PropertyService propertyService;
    private final CustomerService customerService;
    private final UserService userService;
    private final Clock clock;

    public PageResponse<AppointmentResponse> search(AppointmentStatus status, Long propertyId, Long customerId,
                                                    Long agentId, Instant from, Instant to,
                                                    Pageable pageable, UserPrincipal actor) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BusinessException("'from' must be before 'to'");
        }
        Long scopedAgentId = actor.isAdmin() ? agentId : actor.getId();
        Pageable safe = SortSanitizer.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.ASC, "scheduledAt"));
        return PageResponse.of(
                appointmentRepository.search(scopedAgentId, status, propertyId, customerId, from, to, safe),
                appointmentMapper::toResponse);
    }

    public AppointmentResponse getById(Long id, UserPrincipal actor) {
        return appointmentMapper.toResponse(findAccessible(id, actor));
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request, UserPrincipal actor) {
        Property property = propertyService.findProperty(request.propertyId());
        Customer customer = customerService.findAccessible(request.customerId(), actor);
        assertViewable(property);

        User agent = actor.isAdmin() && request.agentId() != null
                ? userService.findAssignableAgent(request.agentId())
                : actor.isAdmin() ? customer.getAgent() : userService.findUser(actor.getId());

        Instant start = request.scheduledAt();
        Instant end = start.plus(duration(request));
        assertNoOverlap(agent.getId(), start, end, null);

        Appointment appointment = Appointment.builder()
                .property(property)
                .customer(customer)
                .agent(agent)
                .scheduledAt(start)
                .endAt(end)
                .status(AppointmentStatus.SCHEDULED)
                .note(request.note())
                .build();
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request, UserPrincipal actor) {
        Appointment appointment = findAccessible(id, actor);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BusinessException("Only scheduled appointments can be edited");
        }
        Property property = appointment.getProperty().getId().equals(request.propertyId())
                ? appointment.getProperty()
                : propertyService.findProperty(request.propertyId());
        assertViewable(property);
        Customer customer = appointment.getCustomer().getId().equals(request.customerId())
                ? appointment.getCustomer()
                : customerService.findAccessible(request.customerId(), actor);
        User agent = actor.isAdmin() && request.agentId() != null
                ? userService.findAssignableAgent(request.agentId())
                : appointment.getAgent();

        Instant start = request.scheduledAt();
        Instant end = start.plus(duration(request));
        assertNoOverlap(agent.getId(), start, end, appointment.getId());

        appointment.setProperty(property);
        appointment.setCustomer(customer);
        appointment.setAgent(agent);
        appointment.setScheduledAt(start);
        appointment.setEndAt(end);
        appointment.setNote(request.note());
        return appointmentMapper.toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse changeStatus(Long id, AppointmentStatus newStatus, String note, UserPrincipal actor) {
        Appointment appointment = findAccessible(id, actor);
        if (appointment.getStatus().isFinal()) {
            throw new BusinessException("Appointment is already %s".formatted(appointment.getStatus()));
        }
        if (newStatus == AppointmentStatus.SCHEDULED) {
            throw new BusinessException("Appointment is already SCHEDULED");
        }
        if ((newStatus == AppointmentStatus.COMPLETED || newStatus == AppointmentStatus.NO_SHOW)
                && appointment.getScheduledAt().isAfter(clock.instant())) {
            throw new BusinessException("An appointment cannot be marked %s before it starts".formatted(newStatus));
        }
        appointment.setStatus(newStatus);
        if (note != null) {
            appointment.setNote(note);
        }
        return appointmentMapper.toResponse(appointment);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        appointmentRepository.delete(findAccessible(id, actor));
    }

    Appointment findAccessible(Long id, UserPrincipal actor) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        if (!actor.isAdmin() && !appointment.getAgent().getId().equals(actor.getId())) {
            throw new ResourceNotFoundException("Appointment", id);
        }
        return appointment;
    }

    private void assertViewable(Property property) {
        if (!VIEWABLE.contains(property.getStatus())) {
            throw new BusinessException("Property is %s and cannot be scheduled for viewing"
                    .formatted(property.getStatus()));
        }
    }

    private void assertNoOverlap(Long agentId, Instant start, Instant end, Long excludeId) {
        if (appointmentRepository.existsOverlapping(agentId, start, end, excludeId)) {
            throw new ConflictException("The agent already has an appointment in this time slot");
        }
    }

    private static Duration duration(AppointmentRequest request) {
        return Duration.ofMinutes(request.durationMinutes() != null
                ? request.durationMinutes() : DEFAULT_DURATION_MINUTES);
    }
}
