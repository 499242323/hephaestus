package olympus.hephaestus.login.log.support;

import olympus.hephaestus.login.log.config.LoginLogCleanupProperties;
import olympus.hephaestus.login.log.service.LoginLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class LoginLogCleanupScheduler {

    private final LoginLogCleanupProperties properties;
    private final LoginLogCleanupNodeMatcher nodeMatcher;
    private final LoginLogService loginLogService;

    public LoginLogCleanupScheduler(LoginLogCleanupProperties properties,
                                    LoginLogCleanupNodeMatcher nodeMatcher,
                                    LoginLogService loginLogService) {
        this.properties = properties;
        this.nodeMatcher = nodeMatcher;
        this.loginLogService = loginLogService;
    }

    @Scheduled(cron = "${hephaestus.login-log.cleanup.cron:0 0 3 * * ?}")
    public void cleanup() {
        if (!nodeMatcher.shouldRun(properties)) {
            log.info("Skip login log cleanup, currentNode={}, enabledNode={}",
                    properties.getCurrentNode(), properties.getEnabledNode());
            return;
        }
        try {
            int retentionDays = Math.max(properties.getRetentionDays(), 1);
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            int deleted = loginLogService.cleanupBefore(cutoffTime);
            log.info("Login log cleanup finished, currentNode={}, cutoffTime={}, deleted={}",
                    properties.getCurrentNode(), cutoffTime, deleted);
        } catch (RuntimeException exception) {
            log.error("Login log cleanup failed, currentNode={}", properties.getCurrentNode(), exception);
        }
    }
}
