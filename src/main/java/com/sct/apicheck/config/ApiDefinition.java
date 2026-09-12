package com.sct.apicheck.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个 API 巡检项定义，对应 apis[]。
 */
public class ApiDefinition {

    private String name;
    private String method;
    private String path;
    private String url;
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> params = new LinkedHashMap<>();
    private JsonNode body;
    private List<Integer> expectedStatus = new ArrayList<>();
    private Integer timeoutMs;
    private Integer maxRtMs;
    private Integer slowThresholdMs;

    @JsonProperty("assert")
    private AssertConfig assertions;

    private Integer retry;
    private List<String> tags = new ArrayList<>();
    private Boolean enabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public JsonNode getBody() {
        return body;
    }

    public void setBody(JsonNode body) {
        this.body = body;
    }

    public List<Integer> getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(List<Integer> expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getMaxRtMs() {
        return maxRtMs;
    }

    public void setMaxRtMs(Integer maxRtMs) {
        this.maxRtMs = maxRtMs;
    }

    public Integer getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(Integer slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    public AssertConfig getAssertions() {
        return assertions;
    }

    public void setAssertions(AssertConfig assertions) {
        this.assertions = assertions;
    }

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}