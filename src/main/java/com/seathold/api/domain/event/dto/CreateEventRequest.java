package com.seathold.api.domain.event.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEventRequest(
                @NotBlank String title,
                String description,
                @NotBlank String venue,
                @NotBlank String city,
                @NotNull LocalDateTime startsAt,
                LocalDateTime endsAt,
                @Min(1) int totalCapacity) {
}
