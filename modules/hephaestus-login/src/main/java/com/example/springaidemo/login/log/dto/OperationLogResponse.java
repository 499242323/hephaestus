package com.example.springaidemo.login.log.dto;

import com.example.springaidemo.login.log.domain.OperationLogEntity;

import java.time.LocalDateTime;

public record OperationLogResponse(
        Long id,
        Long operatorPersonId,
        String operatorName,
        String operatorUsername,
        Long operatorUnitId,
        String operatorUnitName,
        String moduleCode,
        String moduleName,
        String actionCode,
        String actionName,
        String targetType,
        String targetId,
        String targetName,
        Boolean success,
        String summary,
        String detail,
        String clientIp,
        String userAgent,
        String requestUri,
        String requestMethod,
        LocalDateTime createdAt
) {
    public static OperationLogResponse from(OperationLogEntity entity) {
        return new OperationLogResponse(
                entity.getId(),
                entity.getOperatorPersonId(),
                entity.getOperatorName(),
                entity.getOperatorUsername(),
                entity.getOperatorUnitId(),
                entity.getOperatorUnitName(),
                entity.getModuleCode(),
                entity.getModuleName(),
                entity.getActionCode(),
                entity.getActionName(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getTargetName(),
                entity.getSuccessFlag(),
                entity.getSummary(),
                entity.getDetail(),
                entity.getClientIp(),
                entity.getUserAgent(),
                entity.getRequestUri(),
                entity.getRequestMethod(),
                entity.getCreatedAt()
        );
    }
}
