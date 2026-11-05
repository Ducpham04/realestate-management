package com.realestate.user.dto;

import com.realestate.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$", message = "is not a valid phone number") String phone,
        @NotNull Role role,
        @NotNull Boolean enabled
) {
}
