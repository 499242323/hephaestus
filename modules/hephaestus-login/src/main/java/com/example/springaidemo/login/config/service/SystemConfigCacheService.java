package com.example.springaidemo.login.config.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class SystemConfigCacheService {

    private static final String CACHE_KEY_PATTERN = "hephaestus:sys-config:*";
    private static final String VALUE_KEY_PREFIX = "hephaestus:sys-config:value:";
    private static final String PUBLIC_KEY_PREFIX = "hephaestus:sys-config:public:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;

    public SystemConfigCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void evictAllOnStartup() {
        var keys = redisTemplate.keys(CACHE_KEY_PATTERN);
        if (keys == null || keys.isEmpty()) {
            log.info("项目启动完成，Redis 系统配置缓存为空，无需清理");
            return;
        }
        redisTemplate.delete(keys);
        log.info("项目启动完成，已清理 Redis 系统配置缓存，count={}", keys.size());
    }

    public String getValue(String configCode) {
        return redisTemplate.opsForValue().get(VALUE_KEY_PREFIX + configCode);
    }

    public void putValue(String configCode, String value) {
        redisTemplate.opsForValue().set(VALUE_KEY_PREFIX + configCode, value == null ? "" : value, CACHE_TTL);
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
            redisTemplate.expire(key, CACHE_TTL);
        }
    }

    public void evictGroup(String groupCode, Iterable<String> configCodes) {
        redisTemplate.delete(PUBLIC_KEY_PREFIX + groupCode);
        for (String configCode : configCodes) {
            redisTemplate.delete(VALUE_KEY_PREFIX + configCode);
        }
    }
}
