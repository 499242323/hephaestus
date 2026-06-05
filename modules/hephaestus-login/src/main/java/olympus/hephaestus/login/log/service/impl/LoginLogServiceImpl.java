package olympus.hephaestus.login.log.service.impl;

import olympus.hephaestus.login.auth.domain.LoginSessionUser;
import olympus.hephaestus.login.log.constant.LoginLogOperationType;
import olympus.hephaestus.login.log.domain.LoginLogEntity;
import olympus.hephaestus.login.log.dto.LoginLogClientInfo;
import olympus.hephaestus.login.log.dto.LoginLogResponse;
import olympus.hephaestus.login.log.repository.LoginLogRepository;
import olympus.hephaestus.login.log.service.LoginLogService;
import olympus.hephaestus.mybatis.page.PageQuery;
import olympus.hephaestus.mybatis.page.PageSupport;
import olympus.hephaestus.mybatis.page.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogRepository repository;

    public LoginLogServiceImpl(LoginLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordSuccess(LoginSessionUser user, String sessionId, LoginLogClientInfo clientInfo) {
        LoginLogEntity entity = baseEntity(LoginLogOperationType.LOGIN_SUCCESS, true, "登录成功", clientInfo);
        if (user != null) {
            entity.setUsername(truncate(user.username(), 128));
            entity.setPersonId(user.personId());
            entity.setPersonName(truncate(user.personName(), 128));
        }
        entity.setSessionId(truncate(sessionId, 128));
        saveQuietly(entity);
    }

    @Override
    public void recordFailure(String username, String message, LoginLogClientInfo clientInfo) {
        LoginLogEntity entity = baseEntity(LoginLogOperationType.LOGIN_FAILURE, false, message, clientInfo);
        entity.setUsername(truncate(username, 128));
        saveQuietly(entity);
    }

    @Override
    public void recordLogout(LoginSessionUser user, String sessionId, LoginLogClientInfo clientInfo) {
        LoginLogEntity entity = baseEntity(LoginLogOperationType.LOGOUT, true, "退出登录", clientInfo);
        if (user != null) {
            entity.setUsername(truncate(user.username(), 128));
            entity.setPersonId(user.personId());
            entity.setPersonName(truncate(user.personName(), 128));
        }
        entity.setSessionId(truncate(sessionId, 128));
        saveQuietly(entity);
    }

    @Override
    public Pagination<LoginLogResponse> query(String keyword,
                                              String operationType,
                                              Boolean success,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime,
                                              Integer page,
                                              Integer pageSize) {
        PageQuery pageQuery = PageSupport.normalize(page, pageSize);
        List<LoginLogResponse> items = repository.query(
                        normalize(keyword),
                        normalize(operationType),
                        success,
                        startTime,
                        endTime,
                        pageQuery.limit(),
                        pageQuery.offset()
                ).stream()
                .map(LoginLogResponse::from)
                .toList();
        long total = repository.count(normalize(keyword), normalize(operationType), success, startTime, endTime);
        return repository.toPagination(items, total, pageQuery.page(), pageQuery.pageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupBefore(LocalDateTime cutoffTime) {
        if (cutoffTime == null) {
            return 0;
        }
        return repository.deleteBefore(cutoffTime);
    }

    private LoginLogEntity baseEntity(LoginLogOperationType operationType,
                                      boolean success,
                                      String message,
                                      LoginLogClientInfo clientInfo) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setOperationType(operationType.name());
        entity.setSuccessFlag(success);
        entity.setMessage(truncate(message, 512));
        entity.setClientIp(truncate(clientInfo == null ? "unknown" : clientInfo.clientIp(), 64));
        entity.setUserAgent(truncate(clientInfo == null ? "" : clientInfo.userAgent(), 512));
        entity.setRequestUri(truncate(clientInfo == null ? "" : clientInfo.requestUri(), 255));
        return entity;
    }

    private void saveQuietly(LoginLogEntity entity) {
        try {
            repository.insertLog(entity);
        } catch (RuntimeException exception) {
            log.error("Failed to save login log, operationType={}, username={}, clientIp={}",
                    entity.getOperationType(), entity.getUsername(), entity.getClientIp(), exception);
        }
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
