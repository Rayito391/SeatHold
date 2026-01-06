package com.seathold.api.auth.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record LoginResponse(
        String token,
        UUID userId,
        String email,
        String firstName,
        String role) {
}
