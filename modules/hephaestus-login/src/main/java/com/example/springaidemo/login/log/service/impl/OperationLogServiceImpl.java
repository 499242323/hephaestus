package com.example.springaidemo.login.log.service.impl;

import com.example.springaidemo.login.log.domain.OperationLogEntity;
import com.example.springaidemo.login.log.dto.OperationLogResponse;
import com.example.springaidemo.login.log.repository.OperationLogRepository;
import com.example.springaidemo.login.log.service.OperationLogService;
import com.example.springaidemo.mybatis.page.PageQuery;
import com.example.springaidemo.mybatis.page.PageSupport;
import com.example.springaidemo.mybatis.page.Pagination;
import com.example.springaidemo.org.log.dto.OperationLogRecordRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Primary
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogRepository repository;

    public OperationLogServiceImpl(OperationLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordSuccess(OperationLogRecordRequest request) {
        saveQuietly(toEntity(request, true, null));
    }

    @Override
    public void recordFailure(OperationLogRecordRequest request, String message) {
        saveQuietly(toEntity(request, false, message));
    }

    @Override
    public Pagination<OperationLogResponse> query(String keyword,
                                                  String moduleCode,
                                                  String actionCode,
                                                  Boolean success,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  Integer page,
                                                  Integer pageSize) {
        PageQuery pageQuery = PageSupport.normalize(page, pageSize);
        List<OperationLogResponse> items = repository.query(
                        normalize(keyword),
                        normalize(moduleCode),
                        normalize(actionCode),
                        success,
                        startTime,
                        endTime,
                        pageQuery.limit(),
                        pageQuery.offset()
                ).stream()
                .map(OperationLogResponse::from)
                .toList();
        long total = repository.count(normalize(keyword), normalize(moduleCode), normalize(actionCode), success, startTime, endTime);
        return repository.toPagination(items, total, pageQuery.page(), pageQuery.pageSize());
    }

    private OperationLogEntity toEntity(OperationLogRecordRequest request, boolean success, String failureMessage) {
        OperationLogEntity entity = new OperationLogEntity();
        if (request != null) {
            entity.setOperatorPersonId(request.operatorPersonId());
            entity.setOperatorName(truncate(request.operatorName(), 100));
            entity.setOperatorUsername(truncate(request.operatorUsername(), 100));
            entity.setOperatorUnitId(request.operatorUnitId());
            entity.setOperatorUnitName(truncate(request.operatorUnitName(), 200));
            entity.setModuleCode(truncate(requiredText(request.moduleCode(), "unknown"), 64));
            entity.setModuleName(truncate(requiredText(request.moduleName(), "未知模块"), 100));
            entity.setActionCode(truncate(requiredText(request.actionCode(), "unknown"), 64));
            entity.setActionName(truncate(requiredText(request.actionName(), "未知操作"), 100));
            entity.setTargetType(truncate(request.targetType(), 64));
            entity.setTargetId(truncate(request.targetId(), 64));
            entity.setTargetName(truncate(request.targetName(), 200));
            entity.setSummary(truncate(requiredText(request.summary(), "记录操作日志"), 500));
            entity.setDetail(truncate(success ? request.detail() : failureMessage, 2000));
        } else {
            entity.setModuleCode("unknown");
            entity.setModuleName("未知模块");
            entity.setActionCode("unknown");
            entity.setActionName("未知操作");
            entity.setSummary("记录操作日志");
            entity.setDetail(truncate(failureMessage, 2000));
        }
        entity.setSuccessFlag(success);
        fillRequestInfo(entity);
        return entity;
    }

    private void fillRequestInfo(OperationLogEntity entity) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        entity.setClientIp(truncate(resolveClientIp(request), 64));
        entity.setUserAgent(truncate(request.getHeader("User-Agent"), 1000));
        entity.setRequestUri(truncate(request.getRequestURI(), 500));
        entity.setRequestMethod(truncate(request.getMethod(), 16));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void saveQuietly(OperationLogEntity entity) {
        try {
            repository.insertLog(entity);
        } catch (RuntimeException exception) {
            log.error("Failed to save operation log, moduleCode={}, actionCode={}, summary={}",
                    entity.getModuleCode(), entity.getActionCode(), entity.getSummary(), exception);
        }
    }

    private String requiredText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
