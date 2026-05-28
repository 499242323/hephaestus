package com.example.springaidemo.login.config.service;

import com.example.springaidemo.login.config.domain.SystemConfigDefinitionEntity;
import com.example.springaidemo.login.config.domain.SystemConfigValueEntity;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.login.config.dto.SystemConfigFieldResponse;
import com.example.springaidemo.login.config.dto.SystemConfigFormResponse;
import com.example.springaidemo.login.config.dto.SystemConfigOptionResponse;
import com.example.springaidemo.login.config.dto.SystemConfigPublicResponse;
import com.example.springaidemo.login.config.dto.SystemConfigSectionResponse;
import com.example.springaidemo.login.config.repository.SystemConfigDefinitionRepository;
import com.example.springaidemo.login.config.repository.SystemConfigValueRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemConfigService {

    public static final String MAIN_SYSTEM_GROUP = LoginConfigConst.GROUP_MAIN_SYSTEM;

    private final SystemConfigDefinitionRepository definitionRepository;
    private final SystemConfigValueRepository valueRepository;
    private final SystemConfigCacheService cacheService;
    private final ObjectMapper objectMapper;

    public SystemConfigService(SystemConfigDefinitionRepository definitionRepository,
                               SystemConfigValueRepository valueRepository,
                               SystemConfigCacheService cacheService,
                               ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.valueRepository = valueRepository;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    public SystemConfigFormResponse getForm(String groupCode) {
        List<SystemConfigDefinitionEntity> definitions = loadDefinitions(groupCode);
        Map<String, String> values = loadValues(definitions);
        Map<String, List<SystemConfigDefinitionEntity>> sections = definitions.stream()
                .collect(Collectors.groupingBy(
                        definition -> normalize(definition.getSectionCode(), "default"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SystemConfigSectionResponse> sectionResponses = new ArrayList<>();
        for (Map.Entry<String, List<SystemConfigDefinitionEntity>> entry : sections.entrySet()) {
            List<SystemConfigDefinitionEntity> sectionDefinitions = entry.getValue();
            String sectionName = normalize(sectionDefinitions.get(0).getSectionName(), entry.getKey());
            List<SystemConfigFieldResponse> fields = sectionDefinitions.stream()
                    .map(definition -> toField(definition, values.get(definition.getConfigCode())))
                    .toList();
            sectionResponses.add(new SystemConfigSectionResponse(entry.getKey(), sectionName, fields));
        }

        return new SystemConfigFormResponse(groupCode, resolveGroupName(groupCode), sectionResponses);
    }

    public SystemConfigFormResponse saveForm(String groupCode, Map<String, String> values, String updatedBy) {
        List<SystemConfigDefinitionEntity> definitions = loadDefinitions(groupCode);
        Set<String> allowedCodes = definitions.stream()
                .map(SystemConfigDefinitionEntity::getConfigCode)
                .collect(Collectors.toSet());
        for (String code : values.keySet()) {
            if (!allowedCodes.contains(code)) {
                throw new IllegalArgumentException("Unknown config code: " + code);
            }
        }

        Map<String, SystemConfigDefinitionEntity> definitionMap = definitions.stream()
                .collect(Collectors.toMap(SystemConfigDefinitionEntity::getConfigCode, Function.identity()));
        values.forEach((code, value) -> saveValue(definitionMap.get(code), value, updatedBy));
        refreshRedisCache(groupCode, definitions);
        return getForm(groupCode);
    }

    public SystemConfigPublicResponse getPublicConfig(String groupCode) {
        Map<Object, Object> cached = cacheService.getPublicItems(groupCode);
        if (!cached.isEmpty()) {
            Map<String, String> cachedItems = new LinkedHashMap<>();
            cached.forEach((key, value) -> cachedItems.put(String.valueOf(key), String.valueOf(value)));
            return new SystemConfigPublicResponse(groupCode, cachedItems);
        }

        List<SystemConfigDefinitionEntity> definitions = loadDefinitions(groupCode).stream()
                .filter(definition -> Boolean.TRUE.equals(definition.getPublicFlag()))
                .filter(definition -> !Boolean.TRUE.equals(definition.getSensitiveFlag()))
                .toList();
        Map<String, String> values = loadValues(definitions);
        Map<String, String> publicValues = new LinkedHashMap<>();
        for (SystemConfigDefinitionEntity definition : definitions) {
            publicValues.put(definition.getConfigCode(), valueOrDefault(definition, values.get(definition.getConfigCode())));
        }
        cacheService.putPublicItems(groupCode, publicValues);
        return new SystemConfigPublicResponse(groupCode, publicValues);
    }

    public Map<String, String> getPublicItems(String groupCode) {
        return getPublicConfig(groupCode).items();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reloadRedisCacheOnStartup() {
        int evictedCount = cacheService.evictAll();
        List<SystemConfigDefinitionEntity> definitions = loadDefinitions(MAIN_SYSTEM_GROUP);
        int publicCount = refreshRedisCache(MAIN_SYSTEM_GROUP, definitions);
        log.info("Reloaded Redis system config cache on startup, evictedCount={}, valueCount={}, publicCount={}",
                evictedCount, definitions.size(), publicCount);
    }

    private int refreshRedisCache(String groupCode, List<SystemConfigDefinitionEntity> definitions) {
        Map<String, String> values = loadValues(definitions);
        Map<String, String> publicValues = new LinkedHashMap<>();
        for (SystemConfigDefinitionEntity definition : definitions) {
            String value = valueOrDefault(definition, values.get(definition.getConfigCode()));
            cacheService.putValue(definition.getConfigCode(), value);
            if (Boolean.TRUE.equals(definition.getPublicFlag()) && !Boolean.TRUE.equals(definition.getSensitiveFlag())) {
                publicValues.put(definition.getConfigCode(), value);
            }
        }
        cacheService.putPublicItems(groupCode, publicValues);
        return publicValues.size();
    }

    public String getValue(String code, String fallback) {
        String cached = cacheService.getValue(code);
        if (cached != null) {
            return StringUtils.hasText(cached) ? cached : fallback;
        }
        List<SystemConfigDefinitionEntity> definitions = definitionRepository.findEnabledByGroup(MAIN_SYSTEM_GROUP);
        SystemConfigDefinitionEntity definition = definitions.stream()
                .filter(item -> code.equals(item.getConfigCode()))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return fallback;
        }
        Map<String, String> values = loadValues(List.of(definition));
        String value = valueOrDefault(definition, values.get(code));
        cacheService.putValue(code, value);
        return StringUtils.hasText(value) ? value : fallback;
    }

    public boolean getBoolean(String code, boolean fallback) {
        String value = getValue(code, String.valueOf(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    public int getInt(String code, int fallback) {
        String value = getValue(code, String.valueOf(fallback));
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException exception) {
            log.warn("系统配置数字解析失败，configCode={}, value={}, fallback={}", code, value, fallback, exception);
            return fallback;
        }
    }

    private List<SystemConfigDefinitionEntity> loadDefinitions(String groupCode) {
        String normalizedGroup = normalize(groupCode, MAIN_SYSTEM_GROUP);
        return definitionRepository.findEnabledByGroup(normalizedGroup);
    }

    private Map<String, String> loadValues(List<SystemConfigDefinitionEntity> definitions) {
        if (definitions.isEmpty()) {
            return Map.of();
        }
        List<String> codes = definitions.stream().map(SystemConfigDefinitionEntity::getConfigCode).toList();
        return valueRepository.findByCodes(codes).stream()
                .collect(Collectors.toMap(SystemConfigValueEntity::getConfigCode, SystemConfigValueEntity::getConfigValue));
    }

    private void saveValue(SystemConfigDefinitionEntity definition, String value, String updatedBy) {
        if (definition == null) {
            return;
        }
        if (Boolean.TRUE.equals(definition.getRequiredFlag()) && !StringUtils.hasText(value)) {
            throw new IllegalArgumentException(definition.getConfigName() + " cannot be empty");
        }
        if ("number".equalsIgnoreCase(definition.getComponentType()) && StringUtils.hasText(value)) {
            try {
                Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                log.warn("系统配置保存校验失败，数字格式不正确，configCode={}, value={}", definition.getConfigCode(), value, exception);
                throw new IllegalArgumentException(definition.getConfigName() + " must be a number");
            }
        }

        SystemConfigValueEntity entity = new SystemConfigValueEntity();
        entity.setConfigCode(definition.getConfigCode());
        entity.setConfigValue(value == null ? "" : value);
        entity.setUpdatedBy(StringUtils.hasText(updatedBy) ? updatedBy : "system");
        valueRepository.upsert(entity);
    }

    private SystemConfigFieldResponse toField(SystemConfigDefinitionEntity definition, String configuredValue) {
        return new SystemConfigFieldResponse(
                definition.getConfigCode(),
                definition.getConfigName(),
                normalize(definition.getComponentType(), "text"),
                valueOrDefault(definition, configuredValue),
                definition.getDefaultValue(),
                Boolean.TRUE.equals(definition.getRequiredFlag()),
                Boolean.TRUE.equals(definition.getPublicFlag()),
                Boolean.TRUE.equals(definition.getSensitiveFlag()),
                definition.getPlaceholderText(),
                definition.getHelpText(),
                parseOptions(definition.getOptionsJson())
        );
    }

    private List<SystemConfigOptionResponse> parseOptions(String optionsJson) {
        if (!StringUtils.hasText(optionsJson)) {
            return List.of();
        }
        String normalized = optionsJson.trim();
        if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(normalized, new TypeReference<List<SystemConfigOptionResponse>>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("系统配置选项解析失败，optionsJson={}", optionsJson, exception);
            return List.of();
        }
    }

    private String valueOrDefault(SystemConfigDefinitionEntity definition, String configuredValue) {
        return configuredValue != null ? configuredValue : normalize(definition.getDefaultValue(), "");
    }

    private String resolveGroupName(String groupCode) {
        if (MAIN_SYSTEM_GROUP.equals(groupCode)) {
            return "主系统配置";
        }
        return groupCode;
    }

    private String normalize(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
