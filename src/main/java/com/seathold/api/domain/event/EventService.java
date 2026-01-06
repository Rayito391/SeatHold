package com.seathold.api.domain.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.seathold.api.common.exception.BadRequestException;
import com.seathold.api.common.exception.ConflictException;
import com.seathold.api.common.exception.NotFoundException;

@Service
public class EventService {
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    public Event findById(UUID eventId) {
        return getByIdOrThrow(eventId);
    }

    @Transactional
    public Event createDraft(Event event) {
        if (event.getStartsAt() == null) {
            throw new BadRequestException("startsAt is required");
        }
        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("startsAt must be in the future");
        }
        if (event.getTotalCapacity() <= 0) {
            throw new BadRequestException("totalCapacity must be > 0");
        }
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.DRAFT);
        }

        return eventRepository.save(event);
    }

    @Transactional
    public Event publish(UUID eventId) {
        Event event = getByIdOrThrow(eventId);

        if (event.getStatus() == EventStatus.CANCELED) {
            throw new ConflictException("Canceled event cannot be published");
        }

        event.setStatus(EventStatus.PUBLISHED);

        return eventRepository.save(event);

    }

    @Transactional
    public Event cancel(UUID eventId) {
        Event event = getByIdOrThrow(eventId);
        event.setStatus(EventStatus.CANCELED);

        return eventRepository.save(event);
    }

    @Transactional
    public Event update(UUID eventId, Event patch) {
        Event event = getByIdOrThrow(eventId);
        if (patch.getTitle() != null) {
            event.setTitle(patch.getTitle());
        }
        if (patch.getDescription() != null)
            event.setDescription(patch.getDescription());
        if (patch.getVenue() != null)
            event.setVenue(patch.getVenue());
        if (patch.getCity() != null)
            event.setCity(patch.getCity());
        if (patch.getStartsAt() != null)
            event.setStartsAt(patch.getStartsAt());
        event.setEndsAt(patch.getEndsAt());
        if (patch.getTotalCapacity() > 0) {
            event.setTotalCapacity(patch.getTotalCapacity());
        }

        if (event.getStartsAt() != null && event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("startsAt must be in the future");
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void delete(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event not found");
        }
        eventRepository.deleteById(eventId);
    }

    private Event getByIdOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
    }
}
