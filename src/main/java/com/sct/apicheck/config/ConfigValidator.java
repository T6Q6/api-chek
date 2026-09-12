package com.sct.apicheck.config;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 对 {@link ApiCheckConfig} 做必填/合法性校验。
 */
public class ConfigValidator {

    private static final Set<String> ALLOWED_METHODS = Set.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "TRACE", "CONNECT");

    public ValidationResult validate(ApiCheckConfig config) {
        ValidationResult result = new ValidationResult();
        if (config == null) {
            result.addError("配置为空");
            return result;
        }
        if (config.getVersion() < 1) {
            result.addError("version 必须 >= 1");
        }
        List<ApiDefinition> apis = config.getApis();
        if (apis == null || apis.isEmpty()) {
            result.addError("apis 不能为空");
            return result;
        }
        String baseUrl = config.getDefaults() != null ? config.getDefaults().getBaseUrl() : null;
        for (int i = 0; i < apis.size(); i++) {
            ApiDefinition api = apis.get(i);
            if (api == null) {
                result.addError("apis[" + i + "] 不能为空");
                continue;
            }
            String prefix = "apis[" + i + "] " + (api.getName() != null ? "(" + api.getName() + ") " : "");
            validateApi(api, prefix, baseUrl, result);
        }
        return result;
    }

    private void validateApi(ApiDefinition api, String prefix, String baseUrl, ValidationResult result) {
        if (isBlank(api.getName())) {
            result.addError(prefix + "name 为必填项");
        }
        String method = api.getMethod();
        if (isBlank(method)) {
            result.addError(prefix + "method 为必填项");
        } else if (!ALLOWED_METHODS.contains(method.trim().toUpperCase(Locale.ROOT))) {
            result.addError(prefix + "method 不合法: " + method);
        }

        boolean hasPath = !isBlank(api.getPath());
        boolean hasUrl = !isBlank(api.getUrl());
        if (hasPath && hasUrl) {
            result.addError(prefix + "path 与 url 不能同时设置");
        } else if (!hasPath && !hasUrl) {
            result.addError(prefix + "path 或 url 必须设置其一");
        } else if (hasPath && isBlank(baseUrl)) {
            result.addError(prefix + "使用 path 时需要 defaults.base_url");
        }

        List<Integer> status = api.getExpectedStatus();
        if (status == null || status.isEmpty()) {
            result.addError(prefix + "expected_status 为必填项且不能为空");
        } else {
            for (Integer code : status) {
                if (code == null || code < 100 || code > 599) {
                    result.addError(prefix + "expected_status 包含非法状态码: " + code);
                }
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}