package com.realestate.user;

import com.realestate.common.api.ApiResponse;
import com.realestate.common.api.PageResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.user.dto.ChangePasswordRequest;
import com.realestate.user.dto.CreateUserRequest;
import com.realestate.user.dto.UpdateUserRequest;
import com.realestate.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User and agent management")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users (admin)")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @ParameterObject Pageable pageable) {
        return ApiResponse.ok(userService.search(keyword, role, pageable));
    }

    @GetMapping("/agents")
    @Operation(summary = "List active users that can be assigned to records")
    public ApiResponse<PageResponse<UserResponse>> agents(@ParameterObject Pageable pageable) {
        return ApiResponse.ok(userService.listAssignable(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a user (admin)")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("User created", userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a user (admin)")
    public ApiResponse<UserResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateUserRequest request,
                                            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok("User updated", userService.update(id, request, actor));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change own password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UserPrincipal actor,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(actor, request);
        return ApiResponse.ok("Password changed", null);
    }
}
