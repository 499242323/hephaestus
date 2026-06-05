package olympus.hephaestus.login.log.dto;

import olympus.hephaestus.login.log.domain.LoginLogEntity;

import java.time.LocalDateTime;

public record LoginLogResponse(
        Long id,
        String operationType,
        String username,
        Long personId,
        String personName,
        String unitName,
        Boolean success,
        String message,
        String clientIp,
        String userAgent,
        String requestUri,
        LocalDateTime createdAt
) {
    public static LoginLogResponse from(LoginLogEntity entity) {
        return new LoginLogResponse(
                entity.getId(),
                entity.getOperationType(),
                entity.getUsername(),
                entity.getPersonId(),
                entity.getPersonName(),
                entity.getUnitName(),
                entity.getSuccessFlag(),
                entity.getMessage(),
                entity.getClientIp(),
                entity.getUserAgent(),
                entity.getRequestUri(),
                entity.getCreatedAt()
        );
    }
}
