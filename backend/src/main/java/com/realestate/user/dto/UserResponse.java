package com.realestate.user.dto;

import com.realestate.user.Role;
import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
