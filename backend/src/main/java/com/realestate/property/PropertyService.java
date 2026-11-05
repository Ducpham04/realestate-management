package com.realestate.property;

import com.realestate.appointment.AppointmentRepository;
import com.realestate.common.api.PageResponse;
import com.realestate.common.api.SortSanitizer;
import com.realestate.common.exception.BusinessException;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ForbiddenException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.deal.DealRepository;
import com.realestate.property.dto.PropertyFilter;
import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.dto.PropertyResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    static final Set<String> SORTABLE = Set.of("id", "title", "price", "area", "createdAt", "updatedAt", "status",
            "type", "bedrooms");

    private final PropertyRepository propertyRepository;
    private final PropertyMapper propertyMapper;
    private final UserService userService;
    private final AppointmentRepository appointmentRepository;
    private final DealRepository dealRepository;

    public PageResponse<PropertyResponse> search(PropertyFilter filter, Pageable pageable) {
        if (filter.minPrice() != null && filter.maxPrice() != null && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new BusinessException("minPrice must not be greater than maxPrice");
        }
        if (filter.minArea() != null && filter.maxArea() != null && filter.minArea().compareTo(filter.maxArea()) > 0) {
            throw new BusinessException("minArea must not be greater than maxArea");
        }
        Pageable safe = SortSanitizer.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(
                propertyRepository.findAll(PropertySpecifications.withFilter(filter), safe),
                propertyMapper::toResponse);
    }

    public PropertyResponse getById(Long id) {
        return propertyMapper.toResponse(findProperty(id));
    }

    @Transactional
    public PropertyResponse create(PropertyRequest request, UserPrincipal actor) {
        Property property = propertyMapper.toEntity(request);
        property.setStatus(request.status() != null ? request.status() : PropertyStatus.AVAILABLE);
        property.replaceImages(request.images());
        property.setAgent(resolveAgent(request.agentId(), actor));
        return propertyMapper.toResponse(propertyRepository.save(property));
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyRequest request, UserPrincipal actor) {
        Property property = findProperty(id);
        assertCanModify(property, actor);
        propertyMapper.updateEntity(request, property);
        if (request.status() != null) {
            property.setStatus(request.status());
        }
        property.replaceImages(request.images());
        if (request.agentId() != null && actor.isAdmin()) {
            property.setAgent(userService.findAssignableAgent(request.agentId()));
        }
        return propertyMapper.toResponse(property);
    }

    @Transactional
    public PropertyResponse updateStatus(Long id, PropertyStatus status, UserPrincipal actor) {
        Property property = findProperty(id);
        assertCanModify(property, actor);
        property.setStatus(status);
        return propertyMapper.toResponse(property);
    }

    @Transactional
    public void delete(Long id) {
        Property property = findProperty(id);
        if (dealRepository.existsByPropertyId(id) || appointmentRepository.existsByPropertyId(id)) {
            throw new ConflictException("Property has deals or appointments; mark it INACTIVE instead of deleting");
        }
        propertyRepository.delete(property);
    }

    public Property findProperty(Long id) {
        return propertyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Property", id));
    }

    private User resolveAgent(Long requestedAgentId, UserPrincipal actor) {
        if (actor.isAdmin() && requestedAgentId != null) {
            return userService.findAssignableAgent(requestedAgentId);
        }
        return userService.findUser(actor.getId());
    }

    private void assertCanModify(Property property, UserPrincipal actor) {
        if (!actor.isAdmin() && !property.getAgent().getId().equals(actor.getId())) {
            throw new ForbiddenException("You can only modify properties assigned to you");
        }
    }
}
