package com.sct.apicheck.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置校验结果，聚合所有错误信息。
 */
public class ValidationResult {

    private final List<String> errors = new ArrayList<>();

    public void addError(String error) {
        errors.add(error);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}