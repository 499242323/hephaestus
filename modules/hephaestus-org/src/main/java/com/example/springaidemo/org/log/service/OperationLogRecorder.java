package com.example.springaidemo.org.log.service;

import com.example.springaidemo.org.log.dto.OperationLogRecordRequest;

public interface OperationLogRecorder {

    void recordSuccess(OperationLogRecordRequest request);

    void recordFailure(OperationLogRecordRequest request, String message);
}
