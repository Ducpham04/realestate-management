package com.realestate.user.dto;

/** Compact reference to a user embedded in other resources. */
public record UserSummary(Long id, String fullName, String email) {
}
