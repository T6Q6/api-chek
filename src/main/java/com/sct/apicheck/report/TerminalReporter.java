package com.sct.apicheck.report;

import com.sct.apicheck.assertion.Verdict;
import com.sct.apicheck.run.ApiResult;
import com.sct.apicheck.run.RunResult;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 终端巡检报告渲染（PRD §5.2 版式）：每接口一行 + 汇总。
 *
 * <p>渲染为字符串而不是直接打印，便于测试断言与调用方决定输出目标。
 * P0 范围只含「总接口 / 通过 / 失败 / 慢请求 / 成功率」，不含分位数与并发（PLAN §1.2）。</p>
 *
 * <p>抬头时间是给人看的，用本机时区并显式带上偏移量（如 {@code +08:00}），这样直观又不丢时区信息。
 * 注意 {@code Run ID} 与 {@code report.json} 中的时间恒为 UTC，非 UTC 机器上两者看起来会差若干小时，属预期。</p>
 */
public class TerminalReporter {

    /** 用 {@code xxx}（小写）而非 {@code XXX}：零偏移下前者输出 {@code +00:00}，不会突然变成 {@code Z}。 */
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss xxx";

    private final DateTimeFormatter time;

    /** 默认跟随本机时区。 */
    public TerminalReporter() {
        this(ZoneId.systemDefault());
    }

    public TerminalReporter(ZoneId zone) {
        this.time = DateTimeFormatter.ofPattern(TIME_PATTERN, Locale.ROOT).withZone(zone);
    }

    public String render(RunResult result) {
        StringBuilder report = new StringBuilder();
        report.append("API 健康巡检报告\n");
        report.append("Run ID: ").append(result.getRunId()).append('\n');
        report.append("时间: ").append(time.format(result.getStartTime())).append('\n');
        report.append('\n');

        for (ApiResult api : result.getResults()) {
            report.append(renderRow(api)).append('\n');
        }

        report.append('\n');
        report.append("汇总:\n");
        report.append("总接口: ").append(result.getTotal()).append('\n');
        report.append("通过: ").append(result.getPassed()).append('\n');
        report.append("失败: ").append(result.getFailed()).append('\n');
        report.append("慢请求: ").append(result.getSlow()).append('\n');
        report.append("成功率: ").append(formatSuccessRate(result)).append('\n');
        return report.toString();
    }

    private static String renderRow(ApiResult api) {
        return String.format(Locale.ROOT, "%s %-20s %-5s %-24s %-5s %-8s %s",
                marker(api.getVerdict()),
                nullSafe(api.getName()),
                nullSafe(api.getMethod()),
                nullSafe(api.getPath()),
                api.getStatusCode(),
                api.getLatencyMs() + "ms",
                api.getVerdict());
    }

    private static String marker(Verdict verdict) {
        return switch (verdict) {
            case PASS -> "✅";
            case SLOW -> "⚠️";
            case FAIL, ERROR -> "❌";
        };
    }

    private static String formatSuccessRate(RunResult result) {
        return String.format(Locale.ROOT, "%.1f%%", result.getSuccessRate() * 100);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
