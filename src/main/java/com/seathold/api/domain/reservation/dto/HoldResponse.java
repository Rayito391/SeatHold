package com.seathold.api.domain.reservation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HoldResponse(
        UUID reservationId,
        String status,
        LocalDateTime expiresAt) {
}
