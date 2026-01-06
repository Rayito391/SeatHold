package com.seathold.api.domain.reservation;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByIdAndUserId(UUID id, UUID userId);

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

    Page<Reservation> findByUserIdAndStatusAndExpiresAtBefore(
            UUID userId,
            ReservationStatus status,
            LocalDateTime time,
            Pageable pageable);

    Page<Reservation> findByUserIdAndStatusAndExpiresAtAfter(
            UUID userId,
            ReservationStatus status,
            LocalDateTime time,
            Pageable pageable);
}
