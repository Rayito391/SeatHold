package com.seathold.api.domain.reservation.dto;

import jakarta.validation.constraints.Min;

public record HoldRequest(
        @Min(1) int quantity) {
}
