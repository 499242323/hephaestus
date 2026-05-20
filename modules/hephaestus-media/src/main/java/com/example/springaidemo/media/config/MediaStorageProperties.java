package com.example.springaidemo.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public class MediaStorageProperties {

    private String baseUrl = "http://localhost:18080";
    private String username = "egovahttp";
    private String password = "egovahttp";
    private String storagePrefix = "rec";
    private String accessPathPrefix = "/mediadl/media/getdata";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStoragePrefix() {
        return storagePrefix;
    }

    public void setStoragePrefix(String storagePrefix) {
        this.storagePrefix = storagePrefix;
    }

    public String getAccessPathPrefix() {
        return accessPathPrefix;
    }

    public void setAccessPathPrefix(String accessPathPrefix) {
        this.accessPathPrefix = accessPathPrefix;
    }
}
