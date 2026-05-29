package com.example.springaidemo.login.log.service;

import com.example.springaidemo.login.auth.domain.LoginSessionUser;
import com.example.springaidemo.login.log.dto.LoginLogClientInfo;
import com.example.springaidemo.login.log.dto.LoginLogResponse;
import com.example.springaidemo.mybatis.page.Pagination;

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
