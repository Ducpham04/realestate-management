package com.realestate.unit;

import static com.realestate.support.TestData.AGENT_ID;
import static com.realestate.support.TestData.OTHER_AGENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realestate.appointment.Appointment;
import com.realestate.appointment.AppointmentRepository;
import com.realestate.appointment.AppointmentService;
import com.realestate.appointment.AppointmentStatus;
import com.realestate.appointment.dto.AppointmentRequest;
import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.customer.Customer;
import com.realestate.customer.CustomerService;
import com.realestate.property.ListingType;
import com.realestate.property.Property;
import com.realestate.property.PropertyService;
import com.realestate.property.PropertyStatus;
import com.realestate.security.UserPrincipal;
import com.realestate.support.Mappers;
import com.realestate.support.TestData;
import com.realestate.user.Role;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T08:00:00Z");

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PropertyService propertyService;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserService userService;

    private AppointmentService service;
    private final User agent = TestData.user(AGENT_ID, Role.AGENT);
    private final User otherAgent = TestData.user(OTHER_AGENT_ID, Role.AGENT);
    private Property property;
    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, Mappers.APPOINTMENT, propertyService,
                customerService, userService, Clock.fixed(NOW, ZoneOffset.UTC));
        property = TestData.property(10L, otherAgent, PropertyStatus.AVAILABLE, ListingType.SALE);
        customer = TestData.customer(20L, agent);
    }

    private AppointmentRequest request(Instant start, Integer duration) {
        return new AppointmentRequest(10L, 20L, start, duration, "bring documents", null);
    }

    @Test
    void createSchedulesAppointmentWithDefaultDuration() {
        UserPrincipal actor = TestData.agent();
        Instant start = NOW.plus(1, ChronoUnit.DAYS);
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);
        when(userService.findUser(AGENT_ID)).thenReturn(agent);
        when(appointmentRepository.existsOverlapping(AGENT_ID, start, start.plus(60, ChronoUnit.MINUTES), null))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(start, null), actor);

        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.endAt()).isEqualTo(start.plus(60, ChronoUnit.MINUTES));
        assertThat(response.agent().id()).isEqualTo(AGENT_ID);
        assertThat(response.property().id()).isEqualTo(10L);
        assertThat(response.customer().id()).isEqualTo(20L);
    }

    @Test
    void adminWithoutAgentIdAssignsCustomersAgent() {
        UserPrincipal actor = TestData.admin();
        Instant start = NOW.plus(2, ChronoUnit.DAYS);
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(start, 30), actor);

        assertThat(response.agent().id()).isEqualTo(AGENT_ID);
        assertThat(response.endAt()).isEqualTo(start.plus(30, ChronoUnit.MINUTES));
    }

    @Test
    void createRejectsOverlappingSlot() {
        UserPrincipal actor = TestData.agent();
        Instant start = NOW.plus(1, ChronoUnit.DAYS);
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);
        when(userService.findUser(AGENT_ID)).thenReturn(agent);
        when(appointmentRepository.existsOverlapping(eq(AGENT_ID), any(), any(), isNull())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(start, 60), actor)).isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PropertyStatus.class, names = {"SOLD", "RENTED", "INACTIVE"})
    void createRejectsPropertiesThatCannotBeViewed(PropertyStatus status) {
        UserPrincipal actor = TestData.agent();
        property.setStatus(status);
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);

        assertThatThrownBy(() -> service.create(request(NOW.plus(1, ChronoUnit.DAYS), 60), actor))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(status.name());
    }

    @Test
    void cannotCompleteAppointmentBeforeItStarts() {
        Appointment future = TestData.appointment(1L, property, customer, agent,
                NOW.plus(1, ChronoUnit.HOURS), AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> service.changeStatus(1L, AppointmentStatus.COMPLETED, null, TestData.agent()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void completesPastAppointmentAndKeepsNote() {
        Appointment past = TestData.appointment(1L, property, customer, agent,
                NOW.minus(2, ChronoUnit.HOURS), AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(past));

        var response = service.changeStatus(1L, AppointmentStatus.COMPLETED, "liked it", TestData.agent());

        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(past.getNote()).isEqualTo("liked it");
    }

    @Test
    void cancelledAppointmentIsFinal() {
        Appointment cancelled = TestData.appointment(1L, property, customer, agent,
                NOW.plus(1, ChronoUnit.DAYS), AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.changeStatus(1L, AppointmentStatus.SCHEDULED, null, TestData.agent()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.update(1L, request(NOW.plus(2, ChronoUnit.DAYS), 60), TestData.agent()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rescheduleExcludesItselfFromOverlapCheck() {
        Appointment existing = TestData.appointment(1L, property, customer, agent,
                NOW.plus(1, ChronoUnit.DAYS), AppointmentStatus.SCHEDULED);
        Instant newStart = existing.getScheduledAt().plus(30, ChronoUnit.MINUTES);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.existsOverlapping(AGENT_ID, newStart, newStart.plus(60, ChronoUnit.MINUTES), 1L))
                .thenReturn(false);

        service.update(1L, request(newStart, 60), TestData.agent());

        assertThat(existing.getScheduledAt()).isEqualTo(newStart);
        assertThat(existing.getEndAt()).isEqualTo(newStart.plus(60, ChronoUnit.MINUTES));
    }

    @Test
    void searchRejectsInvertedDateRange() {
        assertThatThrownBy(() -> service.search(null, null, null, null, NOW, NOW.minusSeconds(1),
                org.springframework.data.domain.PageRequest.of(0, 10), TestData.admin()))
                .isInstanceOf(BusinessException.class);
    }
}
