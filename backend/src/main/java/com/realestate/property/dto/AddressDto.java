package com.realestate.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressDto(
        @NotBlank @Size(max = 255) String street,
        @Size(max = 100) String ward,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String city
) {
}
