package com.seathold.api.domain.reservation;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;
import com.seathold.api.domain.reservation.dto.ReservationStatusResponse;
import com.seathold.api.security.RoleValidator;
import com.seathold.api.security.RoleValidator.UserInfo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    private final RoleValidator roleValidator;

    public ReservationController(ReservationService reservationService, RoleValidator roleValidator) {
        this.reservationService = reservationService;
        this.roleValidator = roleValidator;
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<ApiResponse<ReservationStatusResponse>> confirm(
            @PathVariable UUID reservationId,
            HttpServletRequest request) {
        roleValidator.requireUser(request);
        UserInfo userInfo = roleValidator.extractUserInfo(request);

        Reservation reservation = reservationService.confirm(reservationId, userInfo.userId());
        ReservationStatusResponse response = new ReservationStatusResponse(
                reservation.getId(),
                reservation.getStatus().name());
        return ApiResponseFactory.successResponse(response);
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<ReservationStatusResponse>> cancel(
            @PathVariable UUID reservationId,
            HttpServletRequest request) {
        roleValidator.requireUser(request);
        UserInfo userInfo = roleValidator.extractUserInfo(request);

        Reservation reservation = reservationService.cancel(reservationId, userInfo.userId());
        ReservationStatusResponse response = new ReservationStatusResponse(
                reservation.getId(),
                reservation.getStatus().name());
        return ApiResponseFactory.successResponse(response);
    }
}
