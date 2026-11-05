package com.realestate.deal;

import com.realestate.common.api.ApiResponse;
import com.realestate.common.api.PageResponse;
import com.realestate.deal.dto.DealRequest;
import com.realestate.deal.dto.DealResponse;
import com.realestate.deal.dto.DealStatusRequest;
import com.realestate.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/deals")
@RequiredArgsConstructor
@Tag(name = "Deals")
public class DealController {

    private final DealService dealService;

    @GetMapping
    @Operation(summary = "Search deals", description = "Agents only see their own deals.")
    public ApiResponse<PageResponse<DealResponse>> search(
            @RequestParam(required = false) DealStatus status,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(dealService.search(status, propertyId, customerId, agentId, minAmount, maxAmount,
                pageable, actor));
    }

    @GetMapping("/{id}")
    public ApiResponse<DealResponse> get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(dealService.getById(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a deal", description = "Reserves the property.")
    public ApiResponse<DealResponse> create(@Valid @RequestBody DealRequest request,
                                            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Deal created", dealService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<DealResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody DealRequest request,
                                            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Deal updated", dealService.update(id, request, actor));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move a deal through the pipeline",
            description = "NEGOTIATING → DEPOSITED → CLOSED, or CANCELLED from any active state.")
    public ApiResponse<DealResponse> changeStatus(@PathVariable Long id,
                                                  @Valid @RequestBody DealStatusRequest request,
                                                  @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Deal status updated", dealService.changeStatus(id, request.status(), actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a non-closed deal (admin)")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        dealService.delete(id, actor);
        return ApiResponse.ok("Deal deleted", null);
    }
}
