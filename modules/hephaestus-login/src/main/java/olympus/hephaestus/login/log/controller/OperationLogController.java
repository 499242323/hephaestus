package olympus.hephaestus.login.log.controller;

import olympus.hephaestus.login.auth.service.AuthService;
import olympus.hephaestus.login.log.dto.OperationLogResponse;
import olympus.hephaestus.login.log.service.OperationLogService;
import olympus.hephaestus.mybatis.page.Pagination;
import olympus.hephaestus.org.role.constant.OrgPermissionCodes;
import olympus.hephaestus.org.role.service.OrgPermissionGuard;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/logs/operations")
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final OrgPermissionGuard permissionGuard;
    private final AuthService authService;

    public OperationLogController(OperationLogService operationLogService,
                                  OrgPermissionGuard permissionGuard,
                                  AuthService authService) {
        this.operationLogService = operationLogService;
        this.permissionGuard = permissionGuard;
        this.authService = authService;
    }

    @GetMapping
    public Pagination<OperationLogResponse> query(HttpSession session,
                                                  @RequestParam(value = "keyword", required = false) String keyword,
                                                  @RequestParam(value = "moduleCode", required = false) String moduleCode,
                                                  @RequestParam(value = "actionCode", required = false) String actionCode,
                                                  @RequestParam(value = "success", required = false) Boolean success,
                                                  @RequestParam(value = "startTime", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                  @RequestParam(value = "endTime", required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                  @RequestParam(value = "page", required = false) Integer page,
                                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Long personId = authService.currentUser(session).personId();
        permissionGuard.requirePermission(personId, OrgPermissionCodes.GENERAL_LOG_OPERATION_VIEW);
        return operationLogService.query(keyword, moduleCode, actionCode, success, startTime, endTime, page, pageSize);
    }
}
