package com.sct.apicheck.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局默认值，可被单个 API 覆盖。
 */
public class DefaultsConfig {

    private String baseUrl;
    private Integer timeoutMs;
    private Integer retry;
    private Integer slowThresholdMs;
    private Map<String, String> headers = new LinkedHashMap<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Integer getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(Integer slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}