package com.example.springaidemo.login.log.service;

import com.example.springaidemo.login.log.dto.OperationLogResponse;
import com.example.springaidemo.mybatis.page.Pagination;
import com.example.springaidemo.org.log.service.OperationLogRecorder;

import java.time.LocalDateTime;

public interface OperationLogService extends OperationLogRecorder {

    Pagination<OperationLogResponse> query(String keyword,
                                           String moduleCode,
                                           String actionCode,
                                           Boolean success,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime,
                                           Integer page,
                                           Integer pageSize);
}
