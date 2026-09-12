package com.sct.apicheck.trace;

/**
 * Span 类型（PRD §5.5）。
 *
 * <p>{@code wireName} 是落盘 JSONL 中 {@code name} 字段的取值。枚举常量名与落盘名分开是有意的：
 * Java 常量必须是合法标识符（{@code API_CALL}），而 PRD 约定的落盘值是 {@code api_call}，
 * 两者的转换集中在这里，别处不再各写各的。</p>
 *
 * <p>Phase 5 只产出 {@code run} / {@code api_call} / {@code assert}；{@code llm_analysis} 由 Phase 6 追加，
 * {@code alert} / {@code persist} 属 P1。枚举是「词汇表」，不等于本阶段都会产出。</p>
 */
public enum SpanType {

    RUN("run"),
    API_CALL("api_call"),
    ASSERT("assert");

    private final String wireName;

    SpanType(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    public static SpanType fromWire(String wireName) {
        SpanType type = tryFromWire(wireName);
        if (type == null) {
            throw new IllegalArgumentException("未知 Span 类型: " + wireName);
        }
        return type;
    }

    /**
     * 与 {@link #fromWire(String)} 同一张表，但认不得时返回 {@code null} 而不是抛异常。
     *
     * <p>读端要的是「跳过不认识的行」（PRD §5.5 的词汇表比实现宽，旧 trace 不能因为升级就再也读不出来），
     * 而「认不得」在写端/测试里是要当场炸出来的错。同一件事的两种态度，分开两个入口表达，
     * 好过让读端去 catch 一个异常来控制流程。</p>
     */
    public static SpanType tryFromWire(String wireName) {
        for (SpanType type : values()) {
            if (type.wireName.equals(wireName)) {
                return type;
            }
        }
        return null;
    }

    /** 便于日志/渲染直接拿到落盘名。 */
    @Override
    public String toString() {
        return wireName;
    }
}
