package org.fia.alumni.alumnifiauesbackend.repository.audit;

import org.fia.alumni.alumnifiauesbackend.entity.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end AND a.actionType IN :actions ORDER BY a.createdAt DESC")
    List<AuditLog> findAccessLogs(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("actions") List<AuditLog.AuditAction> actions
    );

    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.createdAt BETWEEN :start AND :end AND a.actionType IN :actions ORDER BY a.createdAt DESC")
    List<AuditLog> findAccessLogsByUser(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("actions") List<AuditLog.AuditAction> actions
    );
}