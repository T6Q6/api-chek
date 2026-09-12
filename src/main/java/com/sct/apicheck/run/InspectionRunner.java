package com.sct.apicheck.run;

import com.sct.apicheck.assertion.AssertionEngine;
import com.sct.apicheck.assertion.AssertionResult;
import com.sct.apicheck.config.ApiCheckConfig;
import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.http.CallResult;
import com.sct.apicheck.http.HttpCaller;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡检编排：按顺序遍历 {@code apis}，逐个调用 {@link HttpCaller} 交给 {@link AssertionEngine} 判定，
 * 汇总为 {@link RunResult}。
 *
 * <p>单个接口失败（包括调用层抛异常）不影响其余接口继续执行；{@code enabled: false} 的接口被跳过。</p>
 */
public class InspectionRunner {

    private final HttpCaller caller;
    private final AssertionEngine engine;
    private final Clock clock;

    public InspectionRunner(HttpCaller caller, AssertionEngine engine, Clock clock) {
        this.caller = caller;
        this.engine = engine;
        this.clock = clock;
    }

    public RunResult run(ApiCheckConfig config) {
        Instant start = clock.instant();
        String baseUrl = config.getDefaults() == null ? null : config.getDefaults().getBaseUrl();

        List<ApiResult> results = new ArrayList<>();
        for (ApiDefinition api : config.getApis()) {
            if (Boolean.FALSE.equals(api.getEnabled())) {
                continue;
            }
            results.add(inspect(api, baseUrl));
        }

        return new RunResult(new RunIdGenerator(clock).next(), start, clock.instant(), results);
    }

    private ApiResult inspect(ApiDefinition api, String baseUrl) {
        CallResult call;
        try {
            call = caller.call(api, baseUrl);
        } catch (RuntimeException e) {
            call = CallResult.failure(CallResult.ErrorType.UNKNOWN, describe(e), 0);
        }
        AssertionResult assertion = engine.evaluate(api, call);
        return new ApiResult(api, call, assertion);
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
