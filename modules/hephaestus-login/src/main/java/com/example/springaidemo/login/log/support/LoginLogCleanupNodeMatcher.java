package com.example.springaidemo.login.log.support;

import com.example.springaidemo.login.log.config.LoginLogCleanupProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LoginLogCleanupNodeMatcher {

    public boolean shouldRun(LoginLogCleanupProperties properties) {
        if (properties == null || !properties.isEnabled()) {
            return false;
        }
        String currentNode = normalize(properties.getCurrentNode());
        String enabledNode = normalize(properties.getEnabledNode());
        return StringUtils.hasText(currentNode) && currentNode.equals(enabledNode);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
