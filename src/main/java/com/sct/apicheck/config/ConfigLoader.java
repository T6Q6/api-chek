package com.sct.apicheck.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 将 apis.yaml 反序列化为 {@link ApiCheckConfig}。
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
        try {
            return yamlMapper.readValue(Files.readAllBytes(path), ApiCheckConfig.class);
        } catch (IOException e) {
            throw new ConfigException("配置文件解析失败: " + e.getMessage(), e);
        }
    }
}