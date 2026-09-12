package com.sct.apicheck.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 断言配置，对应 apis[].assert。
 *
 * <p>{@code json_path} 的值若为字符串 {@code "not_null"}，表示「该路径存在且非空」的非空断言。</p>
 */
public class AssertConfig {

    private Map<String, Object> jsonPath = new LinkedHashMap<>();
    private String textContains;

    public Map<String, Object> getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(Map<String, Object> jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getTextContains() {
        return textContains;
    }

    public void setTextContains(String textContains) {
        this.textContains = textContains;
    }
}