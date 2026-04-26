package org.fia.alumni.alumnifiauesbackend.repository.event;

import org.fia.alumni.alumnifiauesbackend.entity.event.EventRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    Page<EventRegistration> findByEventId(Long eventId, Pageable pageable);

    long countByEventId(Long eventId);
}