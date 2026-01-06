package com.seathold.api.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByStatusAndCityIgnoreCase(EventStatus status, String city, Pageable pageable);

    Page<Event> findByStatusAndStartsAtBetween(
            EventStatus status,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);

    Page<Event> findByStatusAndCityIgnoreCaseAndStartsAtBetween(
            EventStatus status,
            String city,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable);
}
