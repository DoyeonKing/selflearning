package com.example.springboot.dto.audit;

import com.example.springboot.entity.enums.ActorType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long logId;
    private Integer actorId;
    private ActorType actorType;
    private String action;
    private String targetEntity;
    private Integer targetId;
    private String details;
    private LocalDateTime createdAt;
}
