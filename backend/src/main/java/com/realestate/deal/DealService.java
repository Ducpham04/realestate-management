package com.realestate.deal;

import com.realestate.common.api.PageResponse;
import com.realestate.common.api.SortSanitizer;
import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.customer.Customer;
import com.realestate.customer.CustomerService;
import com.realestate.deal.dto.DealRequest;
import com.realestate.deal.dto.DealResponse;
import com.realestate.property.ListingType;
import com.realestate.property.Property;
import com.realestate.property.PropertyService;
import com.realestate.property.PropertyStatus;
import com.realestate.security.UserPrincipal;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the deal pipeline and keeps the property status in sync with it:
 * opening a deal reserves the property, closing marks it SOLD/RENTED, cancelling releases it.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealService {

    private static final Set<String> SORTABLE = Set.of("id", "amount", "commission", "status", "closedAt",
            "createdAt", "updatedAt");

    private final DealRepository dealRepository;
    private final DealMapper dealMapper;
    private final PropertyService propertyService;
    private final CustomerService customerService;
    private final UserService userService;
    private final Clock clock;

    public PageResponse<DealResponse> search(DealStatus status, Long propertyId, Long customerId, Long agentId,
                                             BigDecimal minAmount, BigDecimal maxAmount,
                                             Pageable pageable, UserPrincipal actor) {
        Long scopedAgentId = actor.isAdmin() ? agentId : actor.getId();
        Pageable safe = SortSanitizer.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(
                dealRepository.search(scopedAgentId, status, propertyId, customerId, minAmount, maxAmount, safe),
                dealMapper::toResponse);
    }

    public DealResponse getById(Long id, UserPrincipal actor) {
        return dealMapper.toResponse(findAccessible(id, actor));
    }

    @Transactional
    public DealResponse create(DealRequest request, UserPrincipal actor) {
        Property property = propertyService.findProperty(request.propertyId());
        Customer customer = customerService.findAccessible(request.customerId(), actor);

        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new BusinessException("Property is %s; a deal can only be opened on an AVAILABLE property"
                    .formatted(property.getStatus()));
        }
        if (dealRepository.existsByPropertyIdAndStatusIn(property.getId(), DealStatus.ACTIVE)) {
            throw new ConflictException("Property already has an active deal");
        }

        User agent = actor.isAdmin() && request.agentId() != null
                ? userService.findAssignableAgent(request.agentId())
                : actor.isAdmin() ? customer.getAgent() : userService.findUser(actor.getId());

        Deal deal = Deal.builder()
                .property(property)
                .customer(customer)
                .agent(agent)
                .amount(request.amount())
                .commission(request.commission())
                .notes(request.notes())
                .status(DealStatus.NEGOTIATING)
                .build();
        property.setStatus(PropertyStatus.RESERVED);
        return dealMapper.toResponse(dealRepository.save(deal));
    }

    @Transactional
    public DealResponse update(Long id, DealRequest request, UserPrincipal actor) {
        Deal deal = findAccessible(id, actor);
        if (!deal.getStatus().isActive()) {
            throw new BusinessException("A %s deal can no longer be edited".formatted(deal.getStatus()));
        }
        if (!deal.getProperty().getId().equals(request.propertyId())) {
            throw new BusinessException("The property of a deal cannot be changed; cancel and open a new deal");
        }
        if (!deal.getCustomer().getId().equals(request.customerId())) {
            deal.setCustomer(customerService.findAccessible(request.customerId(), actor));
        }
        if (actor.isAdmin() && request.agentId() != null) {
            deal.setAgent(userService.findAssignableAgent(request.agentId()));
        }
        deal.setAmount(request.amount());
        deal.setCommission(request.commission());
        deal.setNotes(request.notes());
        return dealMapper.toResponse(deal);
    }

    @Transactional
    public DealResponse changeStatus(Long id, DealStatus target, UserPrincipal actor) {
        Deal deal = findAccessible(id, actor);
        if (!deal.getStatus().canTransitionTo(target)) {
            throw new BusinessException("Cannot change deal status from %s to %s".formatted(deal.getStatus(), target));
        }
        Property property = deal.getProperty();
        switch (target) {
            case CLOSED -> {
                deal.setClosedAt(clock.instant());
                property.setStatus(property.getListingType() == ListingType.RENT
                        ? PropertyStatus.RENTED : PropertyStatus.SOLD);
            }
            case CANCELLED -> releaseProperty(property);
            default -> {
                // NEGOTIATING -> DEPOSITED keeps the property reserved
            }
        }
        deal.setStatus(target);
        return dealMapper.toResponse(deal);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        Deal deal = findAccessible(id, actor);
        if (deal.getStatus() == DealStatus.CLOSED) {
            throw new BusinessException("Closed deals are part of the revenue history and cannot be deleted");
        }
        if (deal.getStatus().isActive()) {
            releaseProperty(deal.getProperty());
        }
        dealRepository.delete(deal);
    }

    Deal findAccessible(Long id, UserPrincipal actor) {
        Deal deal = dealRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Deal", id));
        if (!actor.isAdmin() && !deal.getAgent().getId().equals(actor.getId())) {
            throw new ResourceNotFoundException("Deal", id);
        }
        return deal;
    }

    private static void releaseProperty(Property property) {
        if (property.getStatus() == PropertyStatus.RESERVED) {
            property.setStatus(PropertyStatus.AVAILABLE);
        }
    }
}
