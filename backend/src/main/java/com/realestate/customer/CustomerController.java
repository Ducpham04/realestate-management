package com.realestate.customer;

import com.realestate.common.api.ApiResponse;
import com.realestate.common.api.PageResponse;
import com.realestate.customer.dto.CustomerRequest;
import com.realestate.customer.dto.CustomerResponse;
import com.realestate.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "Search customers", description = "Agents only see their own customers.")
    public ApiResponse<PageResponse<CustomerResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CustomerType type,
            @RequestParam(required = false) Long agentId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(customerService.search(keyword, type, agentId, pageable, actor));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(customerService.getById(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request,
                                                @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Customer created", customerService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CustomerRequest request,
                                                @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Customer updated", customerService.update(id, request, actor));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        customerService.delete(id, actor);
        return ApiResponse.ok("Customer deleted", null);
    }
}
