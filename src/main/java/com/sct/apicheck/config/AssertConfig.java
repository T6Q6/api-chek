package com.sct.apicheck.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 断言配置，对应 apis[].assert。
 */
public class AssertConfig {

    private Map<String, Object> jsonPath = new LinkedHashMap<>();

    public Map<String, Object> getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(Map<String, Object> jsonPath) {
        this.jsonPath = jsonPath;
    }
}