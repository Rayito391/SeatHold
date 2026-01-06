package com.seathold.api.domain.reservation.dto;

import java.util.UUID;

public record ReservationStatusResponse(
        UUID reservationId,
        String status) {
}
