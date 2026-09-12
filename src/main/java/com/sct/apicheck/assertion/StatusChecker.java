package com.sct.apicheck.assertion;

import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.http.CallResult;

import java.util.List;

/**
 * 状态码断言：响应状态码须落在 {@code expected_status} 列表内。
 */
final class StatusChecker implements AssertionChecker {

    @Override
    public void check(ApiDefinition api, CallResult result, List<String> failures) {
        List<Integer> expected = api.getExpectedStatus();
        if (expected == null || expected.isEmpty()) {
            return;
        }
        if (!expected.contains(result.getStatusCode())) {
            failures.add("状态码 " + result.getStatusCode() + " 不在期望列表 " + expected);
        }
    }
}
