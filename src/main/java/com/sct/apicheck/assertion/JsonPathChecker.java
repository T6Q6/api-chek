package com.sct.apicheck.assertion;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.config.AssertConfig;
import com.sct.apicheck.http.CallResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * JSONPath 断言：逐条比对 {@code assert.json_path} 中配置的路径与期望值。
 *
 * <p>特殊期望值 {@code "not_null"} 表示「路径存在且值非 null」。</p>
 *
 * <p>比较规则：数字跨 Integer/Double 按数值比较（{@code 0 == 0.0}）；数字与字符串
 * 视为类型不符；其余按 {@code equals}。</p>
 */
final class JsonPathChecker implements AssertionChecker {

    static final String NOT_NULL = "not_null";

    private static final Configuration JSON_PATH_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonProvider())
            .build();

    @Override
    public void check(ApiDefinition api, CallResult result, List<String> failures) {
        AssertConfig assertions = api.getAssertions();
        if (assertions == null || assertions.getJsonPath() == null || assertions.getJsonPath().isEmpty()) {
            return;
        }
        DocumentContext document = parse(result.getBody());
        if (document == null) {
            failures.add("响应体不是合法 JSON，无法执行 JSONPath 断言");
            return;
        }
        for (Map.Entry<String, Object> entry : assertions.getJsonPath().entrySet()) {
            checkOne(document, entry.getKey(), entry.getValue(), failures);
        }
    }

    private static DocumentContext parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return JsonPath.using(JSON_PATH_CONFIG).parse(body);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static void checkOne(DocumentContext document, String path, Object expected, List<String> failures) {
        Object actual;
        try {
            actual = document.read(path);
        } catch (PathNotFoundException e) {
            failures.add("JSONPath " + path + " 不存在，期望 " + expected);
            return;
        } catch (RuntimeException e) {
            failures.add("JSONPath " + path + " 读取失败: " + e.getMessage());
            return;
        }
        if (NOT_NULL.equals(expected)) {
            if (actual == null) {
                failures.add("JSONPath " + path + " 期望非空，实际为 null");
            }
            return;
        }
        if (!valueEquals(expected, actual)) {
            failures.add("JSONPath " + path + " 期望 " + expected + "，实际 " + actual);
        }
    }

    private static boolean valueEquals(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return expected == actual;
        }
        if (expected instanceof Number left && actual instanceof Number right) {
            try {
                return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString())) == 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (expected instanceof Number || actual instanceof Number) {
            return false;
        }
        return expected.equals(actual);
    }
}
