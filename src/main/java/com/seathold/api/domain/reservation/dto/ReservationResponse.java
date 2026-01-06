package com.seathold.api.domain.reservation.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        UUID eventId,
        int quantity,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {
}
