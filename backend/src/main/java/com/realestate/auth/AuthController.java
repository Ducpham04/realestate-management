package com.realestate.auth;

import com.realestate.auth.dto.AuthResponse;
import com.realestate.auth.dto.LoginRequest;
import com.realestate.auth.dto.RefreshTokenRequest;
import com.realestate.common.api.ApiResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.user.UserService;
import com.realestate.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Log in with email and password")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Logged in", authService.login(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Exchange a refresh token for a new token pair (rotation)")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Revoke a refresh token")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok("Logged out", null);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getById(principal.getId()));
    }
}
