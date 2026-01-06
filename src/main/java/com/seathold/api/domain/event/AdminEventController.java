package com.seathold.api.domain.event;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;
import com.seathold.api.domain.event.dto.CreateEventRequest;
import com.seathold.api.domain.event.dto.EventResponse;
import com.seathold.api.domain.event.dto.UpdateEventRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventService eventService;

    public AdminEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> list() {
        List<EventResponse> events = eventService.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ApiResponseFactory.successResponse(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable UUID id) {
        return ApiResponseFactory.successResponse(toResponse(eventService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest req) {
        Event e = Event.builder()
                .title(req.title())
                .description(req.description())
                .venue(req.venue())
                .city(req.city())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .totalCapacity(req.totalCapacity())
                // TODO: reemplazar por el userId del JWT cuando metamos security
                .createdBy(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .status(EventStatus.DRAFT)
                .build();

        Event created = eventService.createDraft(e);
        return ApiResponseFactory.successResponse(toResponse(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest req) {
        Event patch = Event.builder()
                .title(req.title())
                .description(req.description())
                .venue(req.venue())
                .city(req.city())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .totalCapacity(req.totalCapacity() == null ? 0 : req.totalCapacity())
                .build();

        return ApiResponseFactory.successResponse(toResponse(eventService.update(id, patch)));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publish(@PathVariable UUID id) {
        return ApiResponseFactory.successResponse(toResponse(eventService.publish(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EventResponse>> cancel(@PathVariable UUID id) {
        return ApiResponseFactory.successResponse(toResponse(eventService.cancel(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ApiResponseFactory.successResponse(null, HttpStatus.NO_CONTENT);
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(),
                e.getStatus().name(),
                e.getTitle(),
                e.getDescription(),
                e.getVenue(),
                e.getCity(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getTotalCapacity());
    }
}
