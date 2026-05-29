package com.example.springaidemo.login.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hephaestus.login-log.cleanup")
public class LoginLogCleanupProperties {

    private boolean enabled = true;
    private String currentNode = "default";
    private String enabledNode = "default";
    private int retentionDays = 30;
    private String cron = "0 0 3 * * ?";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public String getEnabledNode() {
        return enabledNode;
    }

    public void setEnabledNode(String enabledNode) {
        this.enabledNode = enabledNode;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
