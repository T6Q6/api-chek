package com.sct.apicheck.trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 把 Span 列表渲染成 {@code trace show} 的时间线文本（版式对齐 PRD §5.5）。
 *
 * <p>与 {@link com.sct.apicheck.report.TerminalReporter} 一样返回字符串而不是直接打印，
 * 便于测试断言、也便于调用方决定输出目标。</p>
 */
public class TraceView {

    /**
     * 渲染时间线：抬头两行 + 若干行「每个非 run 的 Span 一行」。
     *
     * <p>{@code run} Span 不单独成行 —— 它的耗时已经体现在抬头的「Run 总耗时」里，
     * 再列一行只会让人重复读一遍。</p>
     *
     * <p>行按 {@code start_time} 升序排，而不是按传入顺序：它要表达的是「时间轴」，
     * 谁先发生谁在上面，与 Span 的登记顺序解耦。层级不缩进 —— 每行都从 {@code ├─}/{@code └─}
     * 起头，父子的归属靠 {@code api_name} 与相邻行表达，缩进在窄终端里反而挤掉列宽。</p>
     */
    public String render(List<Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return "";
        }

        List<Span> ordered = new ArrayList<>(spans);
        ordered.sort(Comparator.comparing(Span::getStartTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Span run = ordered.stream().filter(span -> span.getType() == SpanType.RUN).findFirst().orElse(null);
        List<Span> rows = ordered.stream().filter(span -> span.getType() != SpanType.RUN).toList();

        StringBuilder timeline = new StringBuilder();
        timeline.append("Trace: ").append(ordered.get(0).getTraceId()).append('\n');
        timeline.append("Run 总耗时: ").append(run == null ? 0L : run.getDurationMs()).append("ms\n");
        timeline.append('\n');

        for (int i = 0; i < rows.size(); i++) {
            timeline.append(i == rows.size() - 1 ? "└─ " : "├─ ");
            timeline.append(row(rows.get(i))).append('\n');
        }
        return timeline.toString();
    }

    private static String row(Span span) {
        return String.format(Locale.ROOT, "%-9s %-12s %6dms  %-5s",
                span.getType(), nullSafe(span.getApiName()), span.getDurationMs(), nullSafe(span.getStatus()));
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
