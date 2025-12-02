package com.example.springboot.service;

import com.example.springboot.dto.audit.AuditLogResponse;
import com.example.springboot.entity.AuditLog;
import com.example.springboot.entity.enums.ActorType;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.AuditLogRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAllAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AuditLogResponse findAuditLogById(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog not found with id " + id));
        return convertToResponseDto(auditLog);
    }

    @Transactional
    public AuditLog createAuditLog(Integer actorId, ActorType actorType, String action, String targetEntity, Integer targetId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setActorType(actorType);
        auditLog.setAction(action);
        auditLog.setTargetEntity(targetEntity);
        auditLog.setTargetId(targetId);
        auditLog.setDetails(details);
        auditLog.setCreatedAt(LocalDateTime.now());
        return auditLogRepository.save(auditLog);
    }

    // 可以添加更多基于查询条件的审计日志查找方法
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findLogsByActor(ActorType actorType, Integer actorId) {
        return auditLogRepository.findByActorTypeAndActorId(actorType, actorId).stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    private AuditLogResponse convertToResponseDto(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        BeanUtils.copyProperties(auditLog, response);
        return response;
    }
}
