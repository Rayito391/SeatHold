package com.seathold.api.auth.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record RegisterResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role,
        String message) {
}
