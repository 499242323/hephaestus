package com.example.springaidemo.org.log.service;

import com.example.springaidemo.org.log.dto.OperationLogRecordRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(OperationLogRecorder.class)
public class NoopOperationLogRecorder implements OperationLogRecorder {

    @Override
    public void recordSuccess(OperationLogRecordRequest request) {
    }

    @Override
    public void recordFailure(OperationLogRecordRequest request, String message) {
    }
}
