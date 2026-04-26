package org.fia.alumni.alumnifiauesbackend.controller.event;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.event.Event;
import org.fia.alumni.alumnifiauesbackend.entity.event.EventRegistration;
import org.fia.alumni.alumnifiauesbackend.repository.event.EventRegistrationRepository;
import org.fia.alumni.alumnifiauesbackend.repository.event.EventRepository;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRepository          eventRepository;
    private final EventRegistrationRepository registrationRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(defaultValue = "false") boolean upcoming,
            @RequestParam(defaultValue = "0")     int page,
            @RequestParam(defaultValue = "10")    int size,
            Authentication auth) {

        Pageable pageable = PageRequest.of(page, size);
        Long userId = (Long) auth.getPrincipal();

        Page<Event> eventsPage = upcoming
                ? eventRepository.findUpcoming(LocalDateTime.now(), pageable)
                : eventRepository.findAllByOrderByStartDateTimeDesc(pageable);

        return ResponseEntity.ok(buildPageResponse(eventsPage, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEvent(
            @PathVariable Long id, Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return eventRepository.findById(id)
                .map(e -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data",    toDto(e, userId))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEvent(
            @RequestBody Map<String, Object> body, Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        Event event = mapToEvent(body, null);
        event.setCreatedByUserId(userId);
        Event saved = eventRepository.save(event);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", "Evento creado exitosamente");
        r.put("data",    toDto(saved, userId));
        return ResponseEntity.ok(r);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return eventRepository.findById(id).map(existing -> {
            mapToEvent(body, existing);
            Event saved = eventRepository.save(existing);
            return ResponseEntity.ok(Map.of("success", true, "data", toDto(saved, userId)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteEvent(@PathVariable Long id) {
        return eventRepository.findById(id).map(event -> {
            event.setStatus(Event.EventStatus.CANCELLED);
            eventRepository.save(event);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true, "message", "Evento cancelado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<Map<String, Object>> register(
            @PathVariable Long id, Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        if (registrationRepository.existsByEventIdAndUserId(id, userId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "Ya estás registrado en este evento"));
        }

        Event event = eventRepository.findById(id).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        if (event.getStatus() != Event.EventStatus.SCHEDULED) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "No es posible registrarse: el evento no está programado"));
        }
        if (event.getStartDateTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "El evento ya comenzó"));
        }
        if (event.getMaxParticipants() != null &&
                event.getCurrentParticipants() >= event.getMaxParticipants()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "El evento ya alcanzó el cupo máximo"));
        }

        EventRegistration reg = EventRegistration.builder()
                .eventId(id).userId(userId).build();
        registrationRepository.save(reg);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", "Registro exitoso");
        r.put("data",    Map.of("eventId", id, "userId", userId,
                "registrationDate", reg.getRegistrationDate()));
        return ResponseEntity.ok(r);
    }

    @DeleteMapping("/{id}/register")
    public ResponseEntity<Map<String, Object>> cancelRegistration(
            @PathVariable Long id, Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return registrationRepository.findByEventIdAndUserId(id, userId).map(reg -> {
            Event event = eventRepository.findById(id).orElse(null);
            if (event != null && event.getStartDateTime().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.<String, Object>of(
                        "success", false, "message", "No puedes cancelar: el evento ya comenzó"));
            }
            registrationRepository.delete(reg);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true, "message", "Registro cancelado exitosamente"));
        }).orElse(ResponseEntity.badRequest().body(Map.of(
                "success", false, "message", "No estás registrado en este evento")));
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<Map<String, Object>> getRegistrations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<EventRegistration> regPage = registrationRepository.findByEventId(id, pageable);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("data",    regPage.getContent().stream().map(this::toRegDto).toList());
        r.put("page",    regPage.getNumber());
        r.put("size",    regPage.getSize());
        r.put("totalElements", regPage.getTotalElements());
        r.put("totalPages",    regPage.getTotalPages());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeEvent(
            @PathVariable Long id, Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        return eventRepository.findById(id).map(event -> {
            event.setStatus(Event.EventStatus.COMPLETED);
            Event saved = eventRepository.save(event);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "Evento marcado como completado",
                    "data", toDto(saved, userId)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/attendance")
    public ResponseEntity<Map<String, Object>> markAttendance(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        @SuppressWarnings("unchecked")
        List<Integer> attendeeIds = (List<Integer>) body.get("attendees");
        if (attendeeIds == null || attendeeIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "Lista de asistentes requerida"));
        }

        int updated = 0;
        for (Integer uid : attendeeIds) {
            registrationRepository.findByEventIdAndUserId(id, uid.longValue())
                    .ifPresent(reg -> {
                        reg.setAttended(true);
                        registrationRepository.save(reg);
                    });
            updated++;
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", updated + " asistencias marcadas"));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        String feedback = (String) body.get("feedback");

        return registrationRepository.findByEventIdAndUserId(id, userId).map(reg -> {
            if (!Boolean.TRUE.equals(reg.getAttended())) {
                return ResponseEntity.badRequest().body(Map.<String, Object>of(
                        "success", false, "message", "Solo puedes dejar feedback si asististe al evento"));
            }
            reg.setFeedback(feedback);
            registrationRepository.save(reg);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true, "message", "Feedback enviado exitosamente"));
        }).orElse(ResponseEntity.badRequest().body(Map.of(
                "success", false, "message", "No estás registrado en este evento")));
    }


    private Map<String, Object> buildPageResponse(Page<Event> page, Long userId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("data",    page.getContent().stream().map(e -> toDto(e, userId)).toList());
        r.put("page",    page.getNumber());
        r.put("size",    page.getSize());
        r.put("totalElements", page.getTotalElements());
        r.put("totalPages",    page.getTotalPages());
        return r;
    }

    private Map<String, Object> toDto(Event e, Long userId) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",               e.getId());
        dto.put("title",            e.getTitle());
        dto.put("description",      e.getDescription());
        dto.put("eventType",        e.getEventType());
        dto.put("locationOrUrl",    e.getLocationOrUrl());
        dto.put("startDateTime",    e.getStartDateTime());
        dto.put("endDateTime",      e.getEndDateTime());
        dto.put("status",           e.getStatus());
        dto.put("maxParticipants",  e.getMaxParticipants());
        dto.put("currentParticipants", e.getCurrentParticipants());
        dto.put("createdAt",        e.getCreatedAt());
        // ¿el usuario actual está registrado?
        if (userId != null) {
            dto.put("isRegistered",
                    registrationRepository.existsByEventIdAndUserId(e.getId(), userId));
        }
        return dto;
    }

    private Map<String, Object> toRegDto(EventRegistration r) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",               r.getId());
        dto.put("eventId",          r.getEventId());
        dto.put("userId",           r.getUserId());
        dto.put("registrationDate", r.getRegistrationDate());
        dto.put("attended",         r.getAttended());
        dto.put("feedback",         r.getFeedback());
        return dto;
    }

    private Event mapToEvent(Map<String, Object> body, Event existing) {
        Event e = existing != null ? existing : Event.builder().build();
        if (body.containsKey("title"))          e.setTitle((String) body.get("title"));
        if (body.containsKey("description"))    e.setDescription((String) body.get("description"));
        if (body.containsKey("locationOrUrl"))  e.setLocationOrUrl((String) body.get("locationOrUrl"));
        if (body.containsKey("eventType"))
            e.setEventType(Event.EventType.valueOf((String) body.get("eventType")));
        if (body.containsKey("startDateTime") && body.get("startDateTime") != null)
            e.setStartDateTime(LocalDateTime.parse((String) body.get("startDateTime")));
        if (body.containsKey("endDateTime") && body.get("endDateTime") != null)
            e.setEndDateTime(LocalDateTime.parse((String) body.get("endDateTime")));
        if (body.containsKey("maxParticipants") && body.get("maxParticipants") != null)
            e.setMaxParticipants((Integer) body.get("maxParticipants"));
        return e;
    }
}