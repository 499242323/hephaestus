package olympus.hephaestus.login.log.service;

import olympus.hephaestus.login.auth.domain.LoginSessionUser;
import olympus.hephaestus.login.log.dto.LoginLogClientInfo;
import olympus.hephaestus.login.log.dto.LoginLogResponse;
import olympus.hephaestus.mybatis.page.Pagination;

import java.time.LocalDateTime;

public interface LoginLogService {

    void recordSuccess(LoginSessionUser user, String sessionId, LoginLogClientInfo clientInfo);

    void recordFailure(String username, String message, LoginLogClientInfo clientInfo);

    void recordLogout(LoginSessionUser user, String sessionId, LoginLogClientInfo clientInfo);

    Pagination<LoginLogResponse> query(String keyword,
                                       String operationType,
                                       Boolean success,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       Integer page,
                                       Integer pageSize);

    int cleanupBefore(LocalDateTime cutoffTime);
}
