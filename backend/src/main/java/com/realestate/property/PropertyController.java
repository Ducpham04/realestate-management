package com.realestate.property;

import com.realestate.common.api.ApiResponse;
import com.realestate.common.api.PageResponse;
import com.realestate.property.dto.PropertyFilter;
import com.realestate.property.dto.PropertyRequest;
import com.realestate.property.dto.PropertyResponse;
import com.realestate.property.dto.PropertyStatusRequest;
import com.realestate.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Tag(name = "Properties")
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    @Operation(summary = "Search properties",
            description = "Filter by keyword, type, listingType, status, city, district, price/area range, "
                    + "minBedrooms, agentId. Paginate with page/size and sort with sort=field,asc|desc.")
    public ApiResponse<PageResponse<PropertyResponse>> search(@ParameterObject PropertyFilter filter,
                                                              @ParameterObject Pageable pageable) {
        return ApiResponse.ok(propertyService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<PropertyResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(propertyService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PropertyResponse> create(@Valid @RequestBody PropertyRequest request,
                                                @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Property created", propertyService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ApiResponse<PropertyResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody PropertyRequest request,
                                                @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Property updated", propertyService.update(id, request, actor));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PropertyResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody PropertyStatusRequest request,
                                                      @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("Status updated", propertyService.updateStatus(id, request.status(), actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a property (admin)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        propertyService.delete(id);
        return ApiResponse.ok("Property deleted", null);
    }
}
