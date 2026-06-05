package olympus.hephaestus.org.log.service;

import olympus.hephaestus.org.log.dto.OperationLogRecordRequest;

public interface OperationLogRecorder {

    void recordSuccess(OperationLogRecordRequest request);

    void recordFailure(OperationLogRecordRequest request, String message);
}
