package com.sct.apicheck.assertion;

import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.config.DefaultsConfig;
import com.sct.apicheck.http.CallResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 断言与判定引擎：给定「接口定义 + 一次调用结果」产出 {@link Verdict}。
 *
 * <p>纯 POJO，不依赖 Spring，也不发起任何网络调用。</p>
 *
 * <p>判定优先级（对齐 PRD §5.3）：传输层异常一律 {@link Verdict#ERROR}；其次任一断言失败为
 * {@link Verdict#FAIL}；断言全过但 RT 超阈值为 {@link Verdict#SLOW}；否则 {@link Verdict#PASS}。</p>
 */
public class AssertionEngine {

    /** 会触发 FAIL 的检查器；RT 不在其中（RT 超阈值是 SLOW）。 */
    private static final List<AssertionChecker> FAILURE_CHECKERS = List.of(
            new StatusChecker(),
            new JsonPathChecker(),
            new TextContainsChecker());

    private final DefaultsConfig defaults;

    public AssertionEngine() {
        this(null);
    }

    /**
     * @param defaults 全局默认值，提供 {@code slow_threshold_ms} 缺省；可为 null
     */
    public AssertionEngine(DefaultsConfig defaults) {
        this.defaults = defaults;
    }

    public AssertionResult evaluate(ApiDefinition api, CallResult result) {
        if (result.isError()) {
            return new AssertionResult(Verdict.ERROR, List.of(errorMessage(result)));
        }

        List<String> failures = new ArrayList<>();
        for (AssertionChecker checker : FAILURE_CHECKERS) {
            checker.check(api, result, failures);
        }
        if (!failures.isEmpty()) {
            return new AssertionResult(Verdict.FAIL, failures);
        }

        // 判断是不是慢接口
        Integer threshold = effectiveSlowThresholdMs(api);
        if (threshold != null && result.getLatencyMs() > threshold) {
            return new AssertionResult(Verdict.SLOW,
                    List.of("RT " + result.getLatencyMs() + "ms 超过阈值 " + threshold + "ms"));
        }
        return AssertionResult.pass();
    }

    /** 阈值取值优先级：api.max_rt_ms &gt; api.slow_threshold_ms &gt; defaults.slow_threshold_ms。 */
    private Integer effectiveSlowThresholdMs(ApiDefinition api) {
        if (api.getMaxRtMs() != null) {
            return api.getMaxRtMs();
        }
        if (api.getSlowThresholdMs() != null) {
            return api.getSlowThresholdMs();
        }
        return defaults == null ? null : defaults.getSlowThresholdMs();
    }

    private static String errorMessage(CallResult result) {
        String message = result.getErrorMessage();
        return message == null || message.isBlank()
                ? "请求失败（" + result.getErrorType() + "）"
                : message;
    }
}
