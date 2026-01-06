package com.seathold.api.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import com.seathold.api.common.exception.BadRequestException;
import com.seathold.api.common.exception.NotFoundException;
import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;
import com.seathold.api.domain.event.dto.EventDetailResponse;
import com.seathold.api.domain.event.dto.EventResponse;
import com.seathold.api.domain.reservation.Reservation;
import com.seathold.api.domain.reservation.ReservationService;
import com.seathold.api.domain.reservation.dto.HoldRequest;
import com.seathold.api.domain.reservation.dto.HoldResponse;
import com.seathold.api.security.RoleValidator;
import com.seathold.api.security.RoleValidator.UserInfo;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    private final ReservationService reservationService;
    private final RoleValidator roleValidator;

    public EventController(EventService eventService, ReservationService reservationService, RoleValidator roleValidator) {
        this.eventService = eventService;
        this.reservationService = reservationService;
        this.roleValidator = roleValidator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> list(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {

        if ((from == null) != (to == null)) {
            throw new BadRequestException("from and to must be provided together");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }

        Page<Event> eventPage = eventService.findEvents(EventStatus.PUBLISHED, city, from, to, pageable);
        Page<EventResponse> response = eventPage.map(this::toResponse);

        log.info("GET /api/events - count: {}", response.getNumberOfElements());
        return ApiResponseFactory.successResponse(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> get(@PathVariable UUID id) {
        Event event = eventService.findById(id);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new NotFoundException("Event not found");
        }

        Integer availableSeats = eventService.getAvailableSeats(id);
        return ApiResponseFactory.successResponse(toDetailResponse(event, availableSeats));
    }

    @PostMapping("/{eventId}/holds")
    public ResponseEntity<ApiResponse<HoldResponse>> hold(
            @PathVariable UUID eventId,
            @Valid @RequestBody HoldRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        roleValidator.requireUser(httpRequest);
        UserInfo userInfo = roleValidator.extractUserInfo(httpRequest);

        Reservation reservation = reservationService.createHold(eventId, userInfo.userId(), request.quantity());
        HoldResponse response = new HoldResponse(
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getExpiresAt());
        return ApiResponseFactory.successResponse(response);
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

    private EventDetailResponse toDetailResponse(Event e, Integer availableSeats) {
        return new EventDetailResponse(
                e.getId(),
                e.getStatus().name(),
                e.getTitle(),
                e.getDescription(),
                e.getVenue(),
                e.getCity(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getTotalCapacity(),
                availableSeats);
    }
}
