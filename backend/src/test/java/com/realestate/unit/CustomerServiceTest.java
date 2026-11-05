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

import com.realestate.appointment.AppointmentRepository;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.customer.Customer;
import com.realestate.customer.CustomerRepository;
import com.realestate.customer.CustomerService;
import com.realestate.customer.CustomerType;
import com.realestate.customer.dto.CustomerRequest;
import com.realestate.deal.DealRepository;
import com.realestate.support.Mappers;
import com.realestate.support.TestData;
import com.realestate.user.Role;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private UserService userService;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DealRepository dealRepository;

    private CustomerService service;
    private final User agent = TestData.user(AGENT_ID, Role.AGENT);
    private final User otherAgent = TestData.user(OTHER_AGENT_ID, Role.AGENT);

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, Mappers.CUSTOMER, userService,
                appointmentRepository, dealRepository);
    }

    private static CustomerRequest request(String phone) {
        return new CustomerRequest("Nguyen Van A", "a@example.com", phone, CustomerType.BUYER,
                new BigDecimal("1000"), new BigDecimal("2000"), "Quan 1", null, null);
    }

    @Test
    void createNormalizesPhoneAndAssignsCurrentAgent() {
        when(customerRepository.existsByPhone("0909123456")).thenReturn(false);
        when(userService.findUser(AGENT_ID)).thenReturn(agent);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request("0909 123-456"), TestData.agent());

        assertThat(response.phone()).isEqualTo("0909123456");
        assertThat(response.agent().id()).isEqualTo(AGENT_ID);
        assertThat(response.budgetMax()).isEqualByComparingTo("2000");
    }

    @Test
    void createRejectsDuplicatePhone() {
        when(customerRepository.existsByPhone("0909123456")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("0909123456"), TestData.agent()))
                .isInstanceOf(ConflictException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void agentSearchIsAlwaysScopedToThemselves() {
        when(customerRepository.search(isNull(), isNull(), eq(AGENT_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.search("  ", null, OTHER_AGENT_ID, PageRequest.of(0, 10), TestData.agent());

        verify(customerRepository).search(isNull(), isNull(), eq(AGENT_ID), any(Pageable.class));
    }

    @Test
    void adminSearchHonoursAgentFilter() {
        when(customerRepository.search(eq("huy"), isNull(), eq(OTHER_AGENT_ID), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.search(" huy ", null, OTHER_AGENT_ID, PageRequest.of(0, 10), TestData.admin());

        verify(customerRepository).search(eq("huy"), isNull(), eq(OTHER_AGENT_ID), any(Pageable.class));
    }

    @Test
    void agentGetsNotFoundForCustomerOfAnotherAgent() {
        when(customerRepository.findById(5L)).thenReturn(Optional.of(TestData.customer(5L, otherAgent)));

        assertThatThrownBy(() -> service.getById(5L, TestData.agent()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminCanReadAnyCustomer() {
        when(customerRepository.findById(5L)).thenReturn(Optional.of(TestData.customer(5L, otherAgent)));

        assertThat(service.getById(5L, TestData.admin()).id()).isEqualTo(5L);
    }

    @Test
    void updateRejectsPhoneOwnedByAnotherCustomer() {
        when(customerRepository.findById(5L)).thenReturn(Optional.of(TestData.customer(5L, agent)));
        when(customerRepository.existsByPhoneAndIdNot("0909123456", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("0909123456"), TestData.agent()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRefusedWhenCustomerHasAppointments() {
        when(customerRepository.findById(5L)).thenReturn(Optional.of(TestData.customer(5L, agent)));
        when(appointmentRepository.existsByCustomerId(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L, TestData.agent())).isInstanceOf(ConflictException.class);
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void budgetRangeValidation() {
        var invalid = new CustomerRequest("A", null, "0909123456", CustomerType.BUYER,
                new BigDecimal("5"), new BigDecimal("1"), null, null, null);
        assertThat(invalid.isBudgetRangeValid()).isFalse();
        assertThat(request("0909123456").isBudgetRangeValid()).isTrue();
    }
}
