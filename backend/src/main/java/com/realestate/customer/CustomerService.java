package com.realestate.customer;

import com.realestate.appointment.AppointmentRepository;
import com.realestate.common.api.PageResponse;
import com.realestate.common.api.SortSanitizer;
import com.realestate.common.exception.ConflictException;
import com.realestate.common.exception.ResourceNotFoundException;
import com.realestate.customer.dto.CustomerRequest;
import com.realestate.deal.DealRepository;
import com.realestate.customer.dto.CustomerResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.user.User;
import com.realestate.user.UserService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Customers are private to their assigned agent; admins can see and manage all of them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private static final Set<String> SORTABLE = Set.of("id", "fullName", "type", "createdAt", "updatedAt");

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserService userService;
    private final AppointmentRepository appointmentRepository;
    private final DealRepository dealRepository;

    public PageResponse<CustomerResponse> search(String keyword, CustomerType type, Long agentId,
                                                 Pageable pageable, UserPrincipal actor) {
        Long scopedAgentId = actor.isAdmin() ? agentId : actor.getId();
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Pageable safe = SortSanitizer.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(customerRepository.search(kw, type, scopedAgentId, safe), customerMapper::toResponse);
    }

    public CustomerResponse getById(Long id, UserPrincipal actor) {
        return customerMapper.toResponse(findAccessible(id, actor));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request, UserPrincipal actor) {
        String phone = normalizePhone(request.phone());
        if (customerRepository.existsByPhone(phone)) {
            throw new ConflictException("A customer with phone '%s' already exists".formatted(phone));
        }
        Customer customer = customerMapper.toEntity(request);
        customer.setPhone(phone);
        customer.setAgent(resolveAgent(request.agentId(), actor));
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request, UserPrincipal actor) {
        Customer customer = findAccessible(id, actor);
        String phone = normalizePhone(request.phone());
        if (customerRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new ConflictException("A customer with phone '%s' already exists".formatted(phone));
        }
        customerMapper.updateEntity(request, customer);
        customer.setPhone(phone);
        if (actor.isAdmin() && request.agentId() != null) {
            customer.setAgent(userService.findAssignableAgent(request.agentId()));
        }
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void delete(Long id, UserPrincipal actor) {
        Customer customer = findAccessible(id, actor);
        if (dealRepository.existsByCustomerId(id) || appointmentRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer has deals or appointments and cannot be deleted");
        }
        customerRepository.delete(customer);
    }

    /**
     * Loads a customer the actor may access. Agents get a 404 for customers of other agents,
     * so the existence of those records is not disclosed.
     */
    public Customer findAccessible(Long id, UserPrincipal actor) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (!actor.isAdmin() && !customer.getAgent().getId().equals(actor.getId())) {
            throw new ResourceNotFoundException("Customer", id);
        }
        return customer;
    }

    private User resolveAgent(Long requestedAgentId, UserPrincipal actor) {
        if (actor.isAdmin() && requestedAgentId != null) {
            return userService.findAssignableAgent(requestedAgentId);
        }
        return userService.findUser(actor.getId());
    }

    private static String normalizePhone(String phone) {
        return phone.replaceAll("[\\s\\-]", "");
    }
}
