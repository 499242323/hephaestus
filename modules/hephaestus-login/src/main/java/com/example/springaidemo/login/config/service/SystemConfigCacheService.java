package com.example.springaidemo.login.config.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SystemConfigCacheService {

    private static final String CACHE_KEY_PATTERN = "hephaestus:sys-config:*";
    private static final String VALUE_KEY_PREFIX = "hephaestus:sys-config:value:";
    private static final String PUBLIC_KEY_PREFIX = "hephaestus:sys-config:public:";

    private final StringRedisTemplate redisTemplate;

    public SystemConfigCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public int evictAll() {
        var keys = redisTemplate.keys(CACHE_KEY_PATTERN);
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        redisTemplate.delete(keys);
        return keys.size();
    }

    public String getValue(String configCode) {
        return redisTemplate.opsForValue().get(VALUE_KEY_PREFIX + configCode);
    }

    public void putValue(String configCode, String value) {
        redisTemplate.opsForValue().set(VALUE_KEY_PREFIX + configCode, value == null ? "" : value);
    }

    public void evictValues(Iterable<String> configCodes) {
        for (String configCode : configCodes) {
            redisTemplate.delete(VALUE_KEY_PREFIX + configCode);
        }
    }

    public Map<Object, Object> getPublicItems(String groupCode) {
        return redisTemplate.opsForHash().entries(PUBLIC_KEY_PREFIX + groupCode);
    }

    public void putPublicItems(String groupCode, Map<String, String> items) {
        String key = PUBLIC_KEY_PREFIX + groupCode;
        redisTemplate.delete(key);
        if (!items.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, items);
        }
    }

    public void evictGroup(String groupCode, Iterable<String> configCodes) {
        redisTemplate.delete(PUBLIC_KEY_PREFIX + groupCode);
        for (String configCode : configCodes) {
            redisTemplate.delete(VALUE_KEY_PREFIX + configCode);
        }
    }
}
