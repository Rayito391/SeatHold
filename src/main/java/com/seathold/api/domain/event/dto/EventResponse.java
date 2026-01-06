package com.seathold.api.domain.event.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String status,
        String title,
        String description,
        String venue,
        String city,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        int totalCapacity) {

}
