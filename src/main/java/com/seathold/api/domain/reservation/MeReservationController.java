package com.seathold.api.domain.reservation;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;
import com.seathold.api.domain.reservation.dto.ReservationResponse;
import com.seathold.api.security.RoleValidator;
import com.seathold.api.security.RoleValidator.UserInfo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/me/reservations")
public class MeReservationController {
    private final ReservationService reservationService;
    private final RoleValidator roleValidator;

    public MeReservationController(ReservationService reservationService, RoleValidator roleValidator) {
        this.reservationService = reservationService;
        this.roleValidator = roleValidator;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {
        roleValidator.requireUser(request);
        UserInfo userInfo = roleValidator.extractUserInfo(request);

        Page<Reservation> reservations = reservationService.listUserReservations(userInfo.userId(), status, pageable);
        Page<ReservationResponse> response = reservations.map(this::toResponse);
        return ApiResponseFactory.successResponse(response);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        String status = reservation.getStatus().name();
        LocalDateTime expiresAt = reservation.getExpiresAt();
        if (reservation.getStatus() == ReservationStatus.HOLD
                && expiresAt != null
                && expiresAt.isBefore(LocalDateTime.now())) {
            status = "EXPIRED";
        }
        return new ReservationResponse(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getQuantity(),
                status,
                expiresAt,
                reservation.getCreatedAt());
    }
}
