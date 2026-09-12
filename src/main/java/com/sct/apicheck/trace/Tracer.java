package com.sct.apicheck.trace;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * 一次巡检的 Span 记录器（PRD §5.5、PLAN §7 Phase 5）。
 *
 * <p>一个实例对应一次 run：{@link #begin(String)} 绑定由 run_id 派生的 trace_id，
 * 之后用 {@link #span} 把每个关键步骤包起来。Span 只留在内存里，落盘交给 {@link TraceWriter}——
 * 这样 {@link Tracer} 不碰文件系统，单测可以只关心层级和耗时。</p>
 *
 * <p>时间一律取构造时传入的 {@link Clock}，因此单测可以用「只在显式推进时才走字」的假时钟，
 * 把 {@code duration_ms} 断言成精确值，而不是去容忍真实时钟的抖动。</p>
 */
public class Tracer {

    private final Clock clock;
    private final List<Span> spans = new ArrayList<>();
    private final Deque<Span> openSpans = new ArrayDeque<>();
    private String traceId;

    public Tracer(Clock clock) {
        this.clock = clock;
    }

    /**
     * 绑定本次巡检的 trace_id —— 由 run_id 派生（{@code run_20260912_101500} → {@code trace_20260912_101500}），
     * 两者一一对应，便于从报告反查链路。
     */
    public void begin(String runId) {
        String suffix = runId.startsWith("run_") ? runId.substring("run_".length()) : runId;
        this.traceId = "trace_" + suffix;
    }

    /**
     * 记录一个 Span：进入时记 start_time，并把新 Span 压为「当前父节点」；离开时补 end_time 与 duration_ms，
     * 并把父节点还原。回调内再调用本方法即自然形成父子关系，不需要手工传 parent_id。
     *
     * <p>{@code work} 抛出异常时同样要收尾（父节点必须还原、end_time 必须落上），否则后续兄弟 Span 会挂错父亲。</p>
     *
     * @param apiName 所属接口名；{@code run} 这类非接口跨度传 {@code null}
     * @param work    在这段 Span 内执行的工作，可直接 setStatus/setInput/setOutput
     * @return {@code work} 的返回值，调用方不必另开变量接收
     */
    public <T> T span(SpanType type, String apiName, Function<Span, T> work) {
        Span span = new Span();
        span.setTraceId(traceId);
        span.setSpanId(String.format(Locale.ROOT, "span_%03d", spans.size() + 1));
        span.setParentId(openSpans.isEmpty() ? null : openSpans.peek().getSpanId());
        span.setType(type);
        span.setApiName(apiName);
        span.setStartTime(clock.instant());

        // 进入即登记（先序），而不是等事后再追加：父 Span 必须排在子 Span 之前，
        // 时间线/落盘才能按「谁包着谁」的顺序读。
        spans.add(span);
        openSpans.push(span);
        try {
            return work.apply(span);
        } finally {
            // 抛异常也要收尾：end_time/时长落上，父节点还原。
            span.setEndTime(clock.instant());
            span.setDurationMs(Duration.between(span.getStartTime(), span.getEndTime()).toMillis());
            openSpans.pop();
        }
    }

    /** 当前绑定的 trace_id。 */
    public String traceId() {
        return traceId;
    }

    /** 已产出的 Span，按「进入顺序」（先序）排列。 */
    public List<Span> spans() {
        return List.copyOf(spans);
    }
}
