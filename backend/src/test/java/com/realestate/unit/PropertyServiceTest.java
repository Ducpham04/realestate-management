package com.realestate.unit;

import static com.realestate.support.TestData.AGENT_ID;
import static com.realestate.support.TestData.OTHER_AGENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.realestate.appointment.AppointmentRepository;
import com.realestate.common.exception.AppException;
import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ForbiddenException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.deal.DealRepository;
import com.realestate.property.ListingType;
import com.realestate.property.Property;
import com.realestate.property.PropertyRepository;
import com.realestate.property.PropertyService;
import com.realestate.property.PropertyStatus;
import com.realestate.property.PropertyType;
import com.realestate.property.dto.AddressDto;
import com.realestate.property.dto.PropertyFilter;
import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.dto.PropertyResponse;
import com.realestate.support.Mappers;
import com.realestate.support.TestData;
import com.realestate.user.Role;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserService userService;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DealRepository dealRepository;

    private PropertyService service;

    private final User agent = TestData.user(AGENT_ID, Role.AGENT);
    private final User otherAgent = TestData.user(OTHER_AGENT_ID, Role.AGENT);

    @BeforeEach
    void setUp() {
        service = new PropertyService(propertyRepository, Mappers.PROPERTY, userService,
                appointmentRepository, dealRepository);
    }

    private static PropertyRequest request(PropertyStatus status, Long agentId, List<String> images) {
        return new PropertyRequest("Nice flat", "desc", PropertyType.APARTMENT, ListingType.SALE, status,
                new BigDecimal("2500000000"), new BigDecimal("75.5"), 2, 2,
                new AddressDto("1 Le Loi", "Ben Nghe", "Quan 1", "Ho Chi Minh"), images, agentId);
    }

    @Nested
    class Create {

        @Test
        void agentCreatesPropertyAssignedToThemselvesWithDefaultStatus() {
            when(userService.findUser(AGENT_ID)).thenReturn(agent);
            when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

            PropertyResponse response = service.create(
                    request(null, OTHER_AGENT_ID, List.of("https://img/1.jpg")), TestData.agent());

            ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
            verify(propertyRepository).save(captor.capture());
            Property saved = captor.getValue();
            assertThat(saved.getAgent()).isSameAs(agent);
            assertThat(saved.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
            assertThat(saved.getImages()).containsExactly("https://img/1.jpg");
            assertThat(saved.getAddress().getCity()).isEqualTo("Ho Chi Minh");
            assertThat(response.title()).isEqualTo("Nice flat");
            assertThat(response.agent().id()).isEqualTo(AGENT_ID);
            verify(userService, never()).findAssignableAgent(any());
        }

        @Test
        void adminCanAssignAnotherAgent() {
            when(userService.findAssignableAgent(OTHER_AGENT_ID)).thenReturn(otherAgent);
            when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

            PropertyResponse response = service.create(
                    request(PropertyStatus.INACTIVE, OTHER_AGENT_ID, null), TestData.admin());

            assertThat(response.agent().id()).isEqualTo(OTHER_AGENT_ID);
            assertThat(response.status()).isEqualTo(PropertyStatus.INACTIVE);
            assertThat(response.images()).isEmpty();
        }
    }

    @Nested
    class Update {

        @Test
        void ownerCanUpdateFieldsAndReplaceImages() {
            Property existing = TestData.property(10L, agent, PropertyStatus.AVAILABLE, ListingType.SALE);
            existing.getImages().add("https://old.jpg");
            when(propertyRepository.findById(10L)).thenReturn(Optional.of(existing));

            PropertyResponse response = service.update(10L,
                    request(PropertyStatus.RESERVED, null, List.of("https://new.jpg")), TestData.agent());

            assertThat(existing.getTitle()).isEqualTo("Nice flat");
            assertThat(existing.getPrice()).isEqualByComparingTo("2500000000");
            assertThat(existing.getStatus()).isEqualTo(PropertyStatus.RESERVED);
            assertThat(existing.getImages()).containsExactly("https://new.jpg");
            assertThat(existing.getId()).isEqualTo(10L);
            assertThat(response.id()).isEqualTo(10L);
        }

        @Test
        void agentCannotUpdatePropertyOfAnotherAgent() {
            Property existing = TestData.property(10L, otherAgent, PropertyStatus.AVAILABLE, ListingType.SALE);
            when(propertyRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.update(10L, request(null, null, null), TestData.agent()))
                    .isInstanceOf(ForbiddenException.class);
            assertThat(existing.getTitle()).isEqualTo("Property 10");
        }

        @Test
        void agentCannotReassignAgent() {
            Property existing = TestData.property(10L, agent, PropertyStatus.AVAILABLE, ListingType.SALE);
            when(propertyRepository.findById(10L)).thenReturn(Optional.of(existing));

            service.update(10L, request(null, OTHER_AGENT_ID, null), TestData.agent());

            assertThat(existing.getAgent()).isSameAs(agent);
        }

        @Test
        void updateStatusThrowsWhenPropertyMissing() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(99L, PropertyStatus.SOLD, TestData.admin()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesPropertyWithoutHistory() {
            Property existing = TestData.property(10L, agent, PropertyStatus.AVAILABLE, ListingType.SALE);
            when(propertyRepository.findById(10L)).thenReturn(Optional.of(existing));

            service.delete(10L);

            verify(propertyRepository).delete(existing);
        }

        @Test
        void refusesToDeletePropertyReferencedByDeals() {
            Property existing = TestData.property(10L, agent, PropertyStatus.SOLD, ListingType.SALE);
            when(propertyRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(dealRepository.existsByPropertyId(10L)).thenReturn(true);

            assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(ConflictException.class);
            verify(propertyRepository, never()).delete(any(Property.class));
        }
    }

    @Nested
    class Search {

        private final PropertyFilter emptyFilter =
                new PropertyFilter(null, null, null, null, null, null, null, null, null, null, null, null);

        @Test
        @SuppressWarnings("unchecked")
        void appliesDefaultSortAndCapsPageSize() {
            when(propertyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(
                            List.of(TestData.property(1L, agent, PropertyStatus.AVAILABLE, ListingType.SALE)),
                            inv.getArgument(1), 1));

            var page = service.search(emptyFilter, PageRequest.of(0, 1000));

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(propertyRepository).findAll(any(Specification.class), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(100);
            assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(1);
        }

        @Test
        void rejectsUnknownSortProperty() {
            assertThatThrownBy(() -> service.search(emptyFilter, PageRequest.of(0, 10, Sort.by("agent.password"))))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Invalid sort property");
        }

        @Test
        void rejectsInvertedPriceRange() {
            var filter = new PropertyFilter(null, null, null, null, null, null,
                    new BigDecimal("10"), new BigDecimal("5"), null, null, null, null);

            assertThatThrownBy(() -> service.search(filter, PageRequest.of(0, 10)))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
