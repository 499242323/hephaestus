package olympus.hephaestus.org.controller;

import olympus.hephaestus.org.exception.OrgAccessDeniedException;
import olympus.hephaestus.org.exception.OrgValidationException;
import olympus.hephaestus.org.role.controller.OrgPermissionController;
import olympus.hephaestus.org.role.controller.OrgRoleController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackageClasses = {OrgUnitController.class, OrgPersonController.class, OrgRoleController.class, OrgPermissionController.class})
public class OrgExceptionHandler {

    @ExceptionHandler(OrgValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(OrgValidationException exception) {
        log.warn("组织设置业务校验异常: {}", exception.getMessage(), exception);
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(OrgAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleAccessDenied(OrgAccessDeniedException exception) {
        log.warn("组织设置权限异常: {}", exception.getMessage(), exception);
        return Map.of("message", exception.getMessage());
    }
}
