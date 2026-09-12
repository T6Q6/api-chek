package com.sct.apicheck.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 apis.yaml 反序列化为 {@link ApiCheckConfig}，并把 defaults 合并进每个接口定义。
 */
public class ConfigLoader {

    private final ObjectMapper yamlMapper;

    public ConfigLoader(ObjectMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    public ApiCheckConfig load(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new ConfigException("配置文件不存在: " + path);
        }
        ApiCheckConfig config;
        try {
            config = yamlMapper.readValue(Files.readAllBytes(path), ApiCheckConfig.class);
        } catch (IOException e) {
            throw new ConfigException("配置文件解析失败: " + e.getMessage(), e);
        }
        applyDefaults(config);
        return config;
    }

    /**
     * 把 defaults 中接口未显式设置的字段补齐——接口自身值优先（如 {@code retry: 0} 表示关闭重试，不被覆盖）。
     */
    private static void applyDefaults(ApiCheckConfig config) {
        DefaultsConfig defaults = config.getDefaults();
        if (defaults == null || config.getApis() == null) {
            return;
        }
        for (ApiDefinition api : config.getApis()) {
            if (api == null) {
                continue;
            }
            if (api.getTimeoutMs() == null) {
                api.setTimeoutMs(defaults.getTimeoutMs());
            }
            if (api.getRetry() == null) {
                api.setRetry(defaults.getRetry());
            }
            if (api.getSlowThresholdMs() == null) {
                api.setSlowThresholdMs(defaults.getSlowThresholdMs());
            }
            api.setHeaders(mergeHeaders(defaults.getHeaders(), api.getHeaders()));
        }
    }

    /** defaults 打底、接口头覆盖；返回新 map，避免与 defaults 共享引用。 */
    private static Map<String, String> mergeHeaders(Map<String, String> defaults, Map<String, String> api) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (defaults != null) {
            merged.putAll(defaults);
        }
        if (api != null) {
            merged.putAll(api);
        }
        return merged;
    }
}
