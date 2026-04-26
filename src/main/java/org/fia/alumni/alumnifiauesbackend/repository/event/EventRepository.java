package org.fia.alumni.alumnifiauesbackend.repository.event;

import org.fia.alumni.alumnifiauesbackend.entity.event.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.status = 'SCHEDULED' AND e.startDateTime > :now ORDER BY e.startDateTime ASC")
    Page<Event> findUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    Page<Event> findAllByOrderByStartDateTimeDesc(Pageable pageable);
}