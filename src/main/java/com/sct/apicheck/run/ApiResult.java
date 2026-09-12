package com.sct.apicheck.run;

import com.sct.apicheck.assertion.AssertionResult;
import com.sct.apicheck.assertion.Verdict;
import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.http.CallResult;

import java.util.List;

/**
 * 单个接口的一次巡检结果：保留原始「接口定义 / 调用结果 / 判定结果」，并提供报告所需的派生视图。
 */
public class ApiResult {

    private final ApiDefinition api;
    private final CallResult call;
    private final AssertionResult assertion;

    public ApiResult(ApiDefinition api, CallResult call, AssertionResult assertion) {
        this.api = api;
        this.call = call;
        this.assertion = assertion;
    }

    public ApiDefinition getApi() {
        return api;
    }

    public CallResult getCall() {
        return call;
    }

    public AssertionResult getAssertion() {
        return assertion;
    }

    public String getName() {
        return api.getName();
    }

    public String getMethod() {
        return api.getMethod();
    }

    public String getPath() {
        if (api.getPath() != null && !api.getPath().isBlank()) {
            return api.getPath();
        }
        return api.getUrl();
    }

    public int getStatusCode() {
        return call.getStatusCode();
    }

    public long getLatencyMs() {
        return call.getLatencyMs();
    }

    public Verdict getVerdict() {
        return assertion.getVerdict();
    }

    public List<String> getMessages() {
        return assertion.getMessages();
    }
}
