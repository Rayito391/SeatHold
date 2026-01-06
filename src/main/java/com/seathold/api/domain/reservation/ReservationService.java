package com.seathold.api.domain.reservation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seathold.api.common.constants.RedisKeys;
import com.seathold.api.common.exception.BadRequestException;
import com.seathold.api.common.exception.ConflictException;
import com.seathold.api.common.exception.NotFoundException;
import com.seathold.api.domain.event.Event;
import com.seathold.api.domain.event.EventService;
import com.seathold.api.domain.event.EventStatus;
import com.seathold.api.redis.AvailabilityService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final EventService eventService;
    private final AvailabilityService availabilityService;
    private final StringRedisTemplate redis;
    private final int holdSeconds;
    private final int lockSeconds;
    private final int maxPerMinute;

    public ReservationService(
            ReservationRepository reservationRepository,
            EventService eventService,
            AvailabilityService availabilityService,
            StringRedisTemplate redis,
            @Value("${app.hold.ttl-seconds:300}") int holdSeconds,
            @Value("${app.hold.lock-seconds:5}") int lockSeconds,
            @Value("${app.hold.rate-limit-per-minute:5}") int maxPerMinute) {
        this.reservationRepository = reservationRepository;
        this.eventService = eventService;
        this.availabilityService = availabilityService;
        this.redis = redis;
        this.holdSeconds = holdSeconds;
        this.lockSeconds = lockSeconds;
        this.maxPerMinute = maxPerMinute;
    }

    @Transactional
    public Reservation createHold(UUID eventId, UUID userId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("quantity must be > 0");
        }

        Event event = eventService.findById(eventId);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new NotFoundException("Event not found");
        }

        checkRateLimit(userId);

        String lockKey = RedisKeys.eventLock(eventId);
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(lockSeconds));
        if (locked == null || !locked) {
            throw new ConflictException("Event is busy");
        }

        try {
            Integer available = availabilityService.getAvailable(eventId);
            if (available == null) {
                availabilityService.init(eventId, event.getTotalCapacity());
            }

            long remaining = availabilityService.decrBy(eventId, quantity);
            if (remaining < 0) {
                availabilityService.incrBy(eventId, quantity);
                throw new ConflictException("Not enough seats");
            }

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(holdSeconds);
            Reservation reservation = Reservation.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .quantity(quantity)
                    .status(ReservationStatus.HOLD)
                    .expiresAt(expiresAt)
                    .build();

            Reservation saved = reservationRepository.save(reservation);
            redis.opsForValue().set(
                    RedisKeys.hold(saved.getId()),
                    eventId + ":" + quantity,
                    Duration.ofSeconds(holdSeconds));

            return saved;
        } finally {
            redis.delete(lockKey);
        }
    }

    @Transactional
    public Reservation confirm(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.HOLD) {
            throw new ConflictException("Reservation cannot be confirmed");
        }

        if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            reservation.setStatus(ReservationStatus.CANCELED);
            Reservation saved = reservationRepository.save(reservation);
            availabilityService.incrBy(reservation.getEventId(), reservation.getQuantity());
            redis.delete(RedisKeys.hold(reservationId));
            throw new ConflictException("Hold expired");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);
        redis.delete(RedisKeys.hold(reservationId));
        return saved;
    }

    @Transactional
    public Reservation cancel(UUID reservationId, UUID userId) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.HOLD) {
            throw new ConflictException("Reservation cannot be canceled");
        }

        reservation.setStatus(ReservationStatus.CANCELED);
        Reservation saved = reservationRepository.save(reservation);
        availabilityService.incrBy(reservation.getEventId(), reservation.getQuantity());
        redis.delete(RedisKeys.hold(reservationId));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Reservation> listUserReservations(UUID userId, String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return reservationRepository.findByUserId(userId, pageable);
        }

        String normalized = status.trim().toUpperCase(Locale.US);
        LocalDateTime now = LocalDateTime.now();

        if ("EXPIRED".equals(normalized)) {
            return reservationRepository.findByUserIdAndStatusAndExpiresAtBefore(
                    userId,
                    ReservationStatus.HOLD,
                    now,
                    pageable);
        }

        ReservationStatus parsed;
        try {
            parsed = ReservationStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status filter");
        }

        if (parsed == ReservationStatus.HOLD) {
            return reservationRepository.findByUserIdAndStatusAndExpiresAtAfter(
                    userId,
                    ReservationStatus.HOLD,
                    now,
                    pageable);
        }

        return reservationRepository.findByUserIdAndStatus(userId, parsed, pageable);
    }

    private void checkRateLimit(UUID userId) {
        String minuteKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String key = RedisKeys.rateLimitUserMinute(userId, minuteKey);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > maxPerMinute) {
            throw new ConflictException("Rate limit exceeded");
        }
    }
}
