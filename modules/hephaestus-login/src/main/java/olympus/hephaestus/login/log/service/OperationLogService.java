package olympus.hephaestus.login.log.service;

import olympus.hephaestus.login.log.dto.OperationLogResponse;
import olympus.hephaestus.mybatis.page.Pagination;
import olympus.hephaestus.org.log.service.OperationLogRecorder;

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
