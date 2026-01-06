package com.seathold.api.domain.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.seathold.api.redis.AvailabilityService;

import lombok.extern.slf4j.Slf4j;

import com.seathold.api.common.exception.BadRequestException;
import com.seathold.api.common.exception.ConflictException;
import com.seathold.api.common.exception.NotFoundException;

@Service
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final AvailabilityService availabilityService;

    public EventService(EventRepository eventRepository, AvailabilityService availabilityService) {
        this.eventRepository = eventRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional(readOnly = true)
    public List<Event> findAll() {
        log.debug("Finding all events");
        List<Event> events = eventRepository.findAll();
        log.info("Found {} events", events.size());
        return events;
    }

    @Transactional(readOnly = true)
    public Event findById(UUID eventId) {
        log.debug("Finding event by id {}", eventId);
        return getByIdOrThrow(eventId);
    }

    @Transactional
    public Event createDraft(Event event) {
        log.info("Creating draft event: title='{}', venue = '{}', capacity ={}", event.getTitle(), event.getVenue(),
                event.getTotalCapacity());

        try {
            validateEventForCreation(event);
            if (event.getStatus() == null) {
                event.setStatus(EventStatus.DRAFT);
            }
            log.debug("Saving event");
            Event saved = eventRepository.save(event);

            log.info("Event created successfully");
            return saved;

        } catch (BadRequestException e) {
            log.error("Event creation failed : {} ", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error : ", e.getMessage());
            throw new RuntimeException("Failed to create event", e);
        }
    }

    @Transactional
    public Event publish(UUID eventId) {
        log.info("Publishing event: {} ", eventId);

        try {
            Event event = getByIdOrThrow(eventId);
            log.debug("Found event for publishing");

            validateEventCanBePublished(event);
            EventStatus oldStatus = event.getStatus();
            event.setStatus(EventStatus.PUBLISHED);
            Event saved = eventRepository.save(event);

            log.info("Event status changed: eventId={}, from={}, to={}", eventId, oldStatus, EventStatus.PUBLISHED);

            initializeAvailabilityAsync(saved.getId(), saved.getTotalCapacity());

            log.info("Event published successfully: eventId={}", eventId);
            return saved;
        } catch (ConflictException e) {
            log.error("Cannot publish event: eventId={}, reason={}", eventId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error publishing event: eventId={}", eventId, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    @Transactional
    public Event cancel(UUID eventId) {
        log.info("Canceling evnt : {} ", eventId);
        try {
            Event event = getByIdOrThrow(eventId);
            log.debug("Found event for cancellation: status = {}, title ='{}'", event.getStatus(), event.getTitle());
            EventStatus oldStatus = event.getStatus();
            event.setStatus(EventStatus.CANCELED);
            Event saved = eventRepository.save(event);
            log.info("Event canceled successfully: eventId={}, previousStatus={}",
                    eventId, oldStatus);
            return saved;

        } catch (Exception e) {
            log.error("Error canceling event: eventId={}", eventId, e);
            throw new RuntimeException("Failed to cancel event", e);
        }
    }

    @Transactional
    public Event update(UUID eventId, Event patch) {
        log.info("Updating event : eventId ={}", eventId);

        try {
            Event event = getByIdOrThrow(eventId);
            log.debug("Current event: status={}, title='{}'", event.getStatus(), event.getTitle());
            boolean hasChanges = applyPatch(event, patch);

            if (!hasChanges) {
                log.debug("No changes for eventId = {}", eventId);
                return event;
            }
            validatedUpdateEvent(event);
            Event saved = eventRepository.save(event);
            log.info("Event updated successfully: eventId={}", eventId);
            return saved;
        } catch (BadRequestException | ConflictException e) {
            log.error("Event update failed:", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating event: eventId={}", eventId, e);
            throw new RuntimeException("Failed to update event", e);
        }
    }

    @Transactional
    public void delete(UUID eventId) {
        log.info("Deleting event: eventId ={}", eventId);
        try {
            Event event = getByIdOrThrow(eventId);
            eventRepository.deleteById(eventId);
            log.info("Event deleted successfully: eventId={}", eventId);
        } catch (ConflictException e) {
            log.error("Cannot delete event: eventId={}", eventId);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting event: eventId={}", eventId, e);
            throw new RuntimeException("Failed to delete event", e);
        }

    }

    private Event getByIdOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Transactional(readOnly = true)
    public Integer getAvailableSeats(UUID eventId) {
        log.debug("Getting available seats: eventId={}", eventId);
        try {
            Integer available = availabilityService.getAvailable(eventId);
            log.debug("Retrieved from redis: eventId={}, available={}", eventId, available);
            return available;
        } catch (Exception e) {
            log.error("Redis unavailable, eventId={}", eventId, e);
            Event event = getByIdOrThrow(eventId);
            return event.getStatus() == EventStatus.PUBLISHED ? event.getTotalCapacity() : null;
        }
    }

    private void validateEventForCreation(Event event) {
        log.debug("Validating event for creation = '{}'", event.getTitle());
        if (event.getStartsAt() == null) {
            throw new BadRequestException("startsAt is required");
        }
        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("startsAt must be in the future");
        }
        if (event.getTotalCapacity() <= 0) {
            throw new BadRequestException("totalCapacity must be > 0");
        }
        if (event.getTotalCapacity() > 10000) {
            throw new BadRequestException("totalCapacity must be < 10,000");
        }
    }

    public void initializeAvailabilityAsync(UUID eventId, int totalCapacity) {
        CompletableFuture.runAsync(() -> {
            try {
                log.debug("Initializing redis availability : eventId {}", eventId);
                availabilityService.init(eventId, totalCapacity);
                log.debug("Redis availability initialized successfully: eventId={}", eventId);
            } catch (Exception e) {
                log.error("Failed to initialize Redis availability:", e);
            }
        });
    }

    public void validateEventCanBePublished(Event event) {
        if (event.getStatus() == EventStatus.CANCELED) {
            throw new ConflictException("Canceled event cannot be published");
        }

        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Cannot publish event that has already started");
        }
    }

    public boolean applyPatch(Event event, Event patch) {
        boolean hasChanges = false;

        if (patch.getTitle() != null && !patch.getTitle().equals(event.getTitle())) {
            event.setTitle(patch.getTitle());
            hasChanges = true;
        }
        if (patch.getDescription() != null && !patch.getDescription().equals(event.getDescription())) {
            event.setDescription(patch.getDescription());
            hasChanges = true;
        }

        if (patch.getVenue() != null && !patch.getVenue().equals(event.getVenue())) {
            event.setVenue(patch.getVenue());
            hasChanges = true;
        }

        if (patch.getCity() != null && !patch.getCity().equals(event.getCity())) {
            event.setCity(patch.getCity());
            hasChanges = true;
        }

        if (patch.getStartsAt() != null && !patch.getStartsAt().equals(event.getStartsAt())) {
            event.setStartsAt(patch.getStartsAt());
            hasChanges = true;
        }

        if (patch.getEndsAt() != null && !patch.getEndsAt().equals(event.getEndsAt())) {
            event.setEndsAt(patch.getEndsAt());
            hasChanges = true;
        }

        if (patch.getTotalCapacity() > 0 && patch.getTotalCapacity() != event.getTotalCapacity()) {
            event.setTotalCapacity(patch.getTotalCapacity());
            hasChanges = true;
        }

        return hasChanges;
    }

    private void validatedUpdateEvent(Event event) {
        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("startsAt must be in the future");
        }
        if (event.getEndsAt() != null && event.getEndsAt().isBefore(event.getEndsAt())) {
            throw new BadRequestException("endsAt must be after startsAt");
        }
        if (event.getStatus() == EventStatus.PUBLISHED && event.getTotalCapacity() <= 0) {
            throw new ConflictException("Cannot change totalCapacity of a published event");
        }
    }

    @Transactional(readOnly = true)
    public Page<Event> findEvents(EventStatus status, String city,
            LocalDateTime from, LocalDateTime to,
            Pageable pageable) {
        log.info("Searching events: status={}, city='{}', from={}, to={}",
                status, city, from, to);

        if (city != null && from != null && to != null) {
            return eventRepository.findByStatusAndCityIgnoreCaseAndStartsAtBetween(
                    status, city, from, to, pageable);
        } else if (city != null) {
            return eventRepository.findByStatusAndCityIgnoreCase(status, city, pageable);
        } else if (from != null && to != null) {
            return eventRepository.findByStatusAndStartsAtBetween(status, from, to, pageable);
        } else {
            return eventRepository.findByStatus(status, pageable);
        }
    }
}
