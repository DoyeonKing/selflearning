package com.example.springboot.repository;

import com.example.springboot.entity.AuditLog;
import com.example.springboot.entity.enums.ActorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByActorTypeAndActorId(ActorType actorType, Integer actorId);
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByTargetEntityAndTargetId(String targetEntity, Integer targetId);
}
