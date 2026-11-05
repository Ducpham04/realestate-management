package com.realestate.unit;

import static com.realestate.support.TestData.AGENT_ID;
import static com.realestate.support.TestData.OTHER_AGENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.customer.Customer;
import com.realestate.customer.CustomerService;
import com.realestate.deal.Deal;
import com.realestate.deal.DealRepository;
import com.realestate.deal.DealService;
import com.realestate.deal.DealStatus;
import com.realestate.deal.dto.DealRequest;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T08:00:00Z");

    @Mock
    private DealRepository dealRepository;
    @Mock
    private PropertyService propertyService;
    @Mock
    private CustomerService customerService;
    @Mock
    private UserService userService;

    private DealService service;
    private final User agent = TestData.user(AGENT_ID, Role.AGENT);
    private final User otherAgent = TestData.user(OTHER_AGENT_ID, Role.AGENT);
    private Property property;
    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new DealService(dealRepository, Mappers.DEAL, propertyService, customerService, userService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        property = TestData.property(10L, agent, PropertyStatus.AVAILABLE, ListingType.SALE);
        customer = TestData.customer(20L, agent);
    }

    private static DealRequest request() {
        return new DealRequest(10L, 20L, new BigDecimal("950000000"), new BigDecimal("19000000"), "note", null);
    }

    @Test
    void createOpensNegotiationAndReservesProperty() {
        UserPrincipal actor = TestData.agent();
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);
        when(dealRepository.existsByPropertyIdAndStatusIn(10L, DealStatus.ACTIVE)).thenReturn(false);
        when(userService.findUser(AGENT_ID)).thenReturn(agent);
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request(), actor);

        assertThat(response.status()).isEqualTo(DealStatus.NEGOTIATING);
        assertThat(response.amount()).isEqualByComparingTo("950000000");
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.RESERVED);
    }

    @Test
    void createRejectsPropertyThatIsNotAvailable() {
        UserPrincipal actor = TestData.agent();
        property.setStatus(PropertyStatus.SOLD);
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);

        assertThatThrownBy(() -> service.create(request(), actor)).isInstanceOf(BusinessException.class);
        verify(dealRepository, never()).save(any());
    }

    @Test
    void createRejectsSecondActiveDeal() {
        UserPrincipal actor = TestData.agent();
        when(propertyService.findProperty(10L)).thenReturn(property);
        when(customerService.findAccessible(20L, actor)).thenReturn(customer);
        when(dealRepository.existsByPropertyIdAndStatusIn(10L, DealStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(), actor)).isInstanceOf(ConflictException.class);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
    }

    @ParameterizedTest
    @CsvSource({"SALE, SOLD", "RENT, RENTED"})
    void closingDealMarksPropertySoldOrRented(ListingType listingType, PropertyStatus expected) {
        property.setListingType(listingType);
        property.setStatus(PropertyStatus.RESERVED);
        Deal deal = TestData.deal(1L, property, customer, agent, DealStatus.DEPOSITED);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));

        var response = service.changeStatus(1L, DealStatus.CLOSED, TestData.agent());

        assertThat(response.status()).isEqualTo(DealStatus.CLOSED);
        assertThat(response.closedAt()).isEqualTo(NOW);
        assertThat(property.getStatus()).isEqualTo(expected);
    }

    @Test
    void cancellingDealReleasesReservedProperty() {
        property.setStatus(PropertyStatus.RESERVED);
        Deal deal = TestData.deal(1L, property, customer, agent, DealStatus.NEGOTIATING);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));

        service.changeStatus(1L, DealStatus.CANCELLED, TestData.agent());

        assertThat(property.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
        assertThat(deal.getClosedAt()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"CLOSED, CANCELLED", "CANCELLED, NEGOTIATING", "DEPOSITED, NEGOTIATING", "NEGOTIATING, NEGOTIATING"})
    void rejectsInvalidTransitions(DealStatus from, DealStatus to) {
        Deal deal = TestData.deal(1L, property, customer, agent, from);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.changeStatus(1L, to, TestData.agent()))
                .isInstanceOf(BusinessException.class);
        assertThat(deal.getStatus()).isEqualTo(from);
    }

    @Test
    void agentCannotSeeDealOfAnotherAgent() {
        Deal deal = TestData.deal(1L, property, customer, otherAgent, DealStatus.NEGOTIATING);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.getById(1L, TestData.agent()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void closedDealCannotBeDeleted() {
        Deal deal = TestData.deal(1L, property, customer, agent, DealStatus.CLOSED);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));

        assertThatThrownBy(() -> service.delete(1L, TestData.admin())).isInstanceOf(BusinessException.class);
        verify(dealRepository, never()).delete(any());
    }

    @Test
    void deletingActiveDealReleasesProperty() {
        property.setStatus(PropertyStatus.RESERVED);
        Deal deal = TestData.deal(1L, property, customer, agent, DealStatus.NEGOTIATING);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));

        service.delete(1L, TestData.admin());

        verify(dealRepository).delete(deal);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
    }

    @Test
    void updateCannotChangeProperty() {
        Deal deal = TestData.deal(1L, property, customer, agent, DealStatus.NEGOTIATING);
        when(dealRepository.findById(1L)).thenReturn(Optional.of(deal));
        var moved = new DealRequest(99L, 20L, BigDecimal.TEN, BigDecimal.ONE, null, null);

        assertThatThrownBy(() -> service.update(1L, moved, TestData.agent())).isInstanceOf(BusinessException.class);
    }

    @Test
    void commissionMayNotExceedAmount() {
        assertThat(new DealRequest(1L, 1L, BigDecimal.ONE, BigDecimal.TEN, null, null).isCommissionValid()).isFalse();
        assertThat(request().isCommissionValid()).isTrue();
    }
}
