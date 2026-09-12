package com.sct.apicheck.run;

import com.sct.apicheck.assertion.AssertionEngine;
import com.sct.apicheck.assertion.AssertionResult;
import com.sct.apicheck.config.ApiCheckConfig;
import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.http.CallResult;
import com.sct.apicheck.http.HttpCaller;
import com.sct.apicheck.trace.SpanType;
import com.sct.apicheck.trace.Tracer;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 巡检编排：按顺序遍历 {@code apis}，逐个调用 {@link HttpCaller} 交给 {@link AssertionEngine} 判定，
 * 汇总为 {@link RunResult}。
 *
 * <p>单个接口失败（包括调用层抛异常）不影响其余接口继续执行；{@code enabled: false} 的接口被跳过。</p>
 *
 * <p>链路追踪（Phase 5）在此埋点：{@code run()} 先 mint run_id 并 {@code tracer.begin(runId)}，
 * 再用 {@code tracer.span(...)} 依次包住 run / api_call / assert 三层。</p>
 */
public class InspectionRunner {

    private final HttpCaller caller;
    private final AssertionEngine engine;
    private final Tracer tracer;
    private final Clock clock;

    public InspectionRunner(HttpCaller caller, AssertionEngine engine, Tracer tracer, Clock clock) {
        this.caller = caller;
        this.engine = engine;
        this.tracer = tracer;
        this.clock = clock;
    }

    public RunResult run(ApiCheckConfig config) {
        String runId = new RunIdGenerator(clock).next();
        tracer.begin(runId);

        return tracer.span(SpanType.RUN, null, runSpan -> {
            Instant start = clock.instant();
            String baseUrl = config.getDefaults() == null ? null : config.getDefaults().getBaseUrl();

            List<ApiResult> results = new ArrayList<>();
            for (ApiDefinition api : config.getApis()) {
                if (Boolean.FALSE.equals(api.getEnabled())) {
                    continue;
                }
                results.add(inspect(api, baseUrl));
            }

            RunResult result = new RunResult(runId, start, clock.instant(), results);
            runSpan.setStatus(result.getFailed() > 0 ? "FAIL" : "PASS");
            return result;
        });
    }

    private ApiResult inspect(ApiDefinition api, String baseUrl) {
        return tracer.span(SpanType.API_CALL, api.getName(), callSpan -> {
            CallResult call = callApi(api, baseUrl);

            AssertionResult assertion = tracer.span(SpanType.ASSERT, api.getName(), assertSpan -> {
                AssertionResult evaluated = engine.evaluate(api, call);
                assertSpan.setStatus(evaluated.getVerdict().name());
                return evaluated;
            });

            ApiResult result = new ApiResult(api, call, assertion);
            callSpan.setStatus(result.getVerdict().name());
            callSpan.setInput(Map.of("method", result.getMethod(), "url", result.getPath()));
            callSpan.setOutput(Map.of("status_code", call.getStatusCode()));
            return result;
        });
    }

    /** 调用层抛异常时归一化为 {@link CallResult#failure}，让上层只需面对一种结果类型。 */
    private CallResult callApi(ApiDefinition api, String baseUrl) {
        try {
            return caller.call(api, baseUrl);
        } catch (RuntimeException e) {
            return CallResult.failure(CallResult.ErrorType.UNKNOWN, describe(e), 0);
        }
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
