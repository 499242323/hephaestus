package com.example.springaidemo.login.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "hephaestus.auth")
public class LoginAuthProperties {

    private final Whitelist whitelist = new Whitelist();

    public Whitelist getWhitelist() {
        return whitelist;
    }

    public static class Whitelist {
        private List<String> paths = new ArrayList<>();

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths == null ? new ArrayList<>() : paths;
        }
    }
}
