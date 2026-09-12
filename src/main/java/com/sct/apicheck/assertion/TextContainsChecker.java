package com.sct.apicheck.assertion;

import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.config.AssertConfig;
import com.sct.apicheck.http.CallResult;

import java.util.List;

/**
 * 文本包含断言：响应体须包含 {@code text_contains} 指定的子串。
 */
final class TextContainsChecker implements AssertionChecker {

    @Override
    public void check(ApiDefinition api, CallResult result, List<String> failures) {
        AssertConfig assertions = api.getAssertions();
        if (assertions == null) {
            return;
        }
        String expected = assertions.getTextContains();
        if (expected == null || expected.isEmpty()) {
            return;
        }
        String body = result.getBody();
        if (body == null || !body.contains(expected)) {
            failures.add("响应体不包含 '" + expected + "'");
        }
    }
}
