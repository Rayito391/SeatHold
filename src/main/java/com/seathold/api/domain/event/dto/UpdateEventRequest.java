package com.seathold.api.domain.event.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;

public record UpdateEventRequest(
                String title,
                String description,
                String venue,
                String city,
                LocalDateTime startsAt,
                LocalDateTime endsAt,
                @Min(0) Integer totalCapacity) {
}
