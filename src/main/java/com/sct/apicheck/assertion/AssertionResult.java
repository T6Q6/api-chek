package com.sct.apicheck.assertion;

import java.util.List;

/**
 * 断言引擎的判定产物：结论 + 人类可读的原因清单。
 *
 * <p>{@code messages} 的语义随 {@link Verdict} 变化：
 * PASS 为空；FAIL 逐条列出失败的检查项；SLOW 为超阈值说明；ERROR 为传输层错误描述。</p>
 */
public class AssertionResult {

    private final Verdict verdict;
    private final List<String> messages;

    public AssertionResult(Verdict verdict, List<String> messages) {
        this.verdict = verdict;
        this.messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static AssertionResult pass() {
        return new AssertionResult(Verdict.PASS, List.of());
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public List<String> getMessages() {
        return messages;
    }
}
