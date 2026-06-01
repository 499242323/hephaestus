package com.example.springaidemo.org.log.dto;

public record OperationLogRecordRequest(
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
        String summary,
        String detail
) {
}
