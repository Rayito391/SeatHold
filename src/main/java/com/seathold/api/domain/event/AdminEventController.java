package com.seathold.api.domain.event;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;
import com.seathold.api.domain.event.dto.CreateEventRequest;
import com.seathold.api.domain.event.dto.EventResponse;
import com.seathold.api.domain.event.dto.UpdateEventRequest;
import com.seathold.api.security.JwtService;
import com.seathold.api.security.RoleValidator;
import com.seathold.api.security.RoleValidator.UserInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final EventService eventService;
    private final RoleValidator roleValidator;

    public AdminEventController(EventService eventService, RoleValidator roleValidator) {
        this.eventService = eventService;
        this.roleValidator = roleValidator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> list(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/admin/events - status: {}, city: {}, page: {}",
                status, city, pageable.getPageNumber());
        List<EventResponse> events;

        if (status != null || city != null) {
            Page<Event> eventPage = eventService.findEvents(status, city, null, null, pageable);
            events = eventPage.getContent()
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            events = eventService.findAll()
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        log.info("Found {} events", events.size());
        return ApiResponseFactory.successResponse(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable UUID id) {
        log.info("GET /api/admin/events/{}", id);
        Event event = eventService.findById(id);
        return ApiResponseFactory.successResponse(toResponse(event));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest req,
            HttpServletRequest request) {
        log.info("POST /api/admin/events - title: '{}'", req.title());

        roleValidator.requireAdminRole(request);

        UserInfo userInfo = roleValidator.extractUserInfo(request);

        log.info("POST /api/admin/events - title: '{}' with ADMIN: {}",
                req.title(), userInfo.email());

        Event e = Event.builder()
                .title(req.title())
                .description(req.description())
                .venue(req.venue())
                .city(req.city())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .totalCapacity(req.totalCapacity())
                .createdBy(userInfo.userId())
                .status(EventStatus.DRAFT)
                .build();

        Event created = eventService.createDraft(e);
        log.info("Event created successfully: id={}", created.getId());
        return ApiResponseFactory.successResponse(toResponse(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest req, HttpServletRequest request) {
        log.info("PUT /api/admin/events/{}", id);

        roleValidator.requireAdminRole(request);

        UserInfo userInfo = roleValidator.extractUserInfo(request);

        log.info("PUT /api/admin/events - title: '{}' with ADMIN: {}",
                req.title(), userInfo.email());

        Event patch = Event.builder()
                .title(req.title())
                .description(req.description())
                .venue(req.venue())
                .city(req.city())
                .startsAt(req.startsAt())
                .endsAt(req.endsAt())
                .totalCapacity(req.totalCapacity() == null ? 0 : req.totalCapacity())
                .build();
        Event updated = eventService.update(id, patch);
        log.info("Event updated successfully: id={}", id);

        return ApiResponseFactory.successResponse(toResponse(updated));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publish(@PathVariable UUID id, HttpServletRequest request) {
        roleValidator.requireAdminRole(request);
        log.info("POST /api/admin/events/{}/publish", id);

        UserInfo userInfo = roleValidator.extractUserInfo(request);

        log.info("POST /api/admin/events/{}/publish with ADMIN: {}", id, userInfo.email());

        Event publish = eventService.publish(id);
        log.info("Event published successfully: id={}", id);

        return ApiResponseFactory.successResponse(toResponse(publish));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EventResponse>> cancel(@PathVariable UUID id, HttpServletRequest request) {
        log.info("POST /api/admin/events/{}/cancel", id);
        roleValidator.requireAdminRole(request);

        Event canceled = eventService.cancel(id);
        log.info("Event cancelled successfully: id={}", id);

        return ApiResponseFactory.successResponse(toResponse(canceled));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, HttpServletRequest request) {

        log.info("DELETE /api/admin/events/{}", id);
        roleValidator.requireAdminRole(request);

        eventService.delete(id);
        log.info("Event deleted successfully: id={}", id);
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