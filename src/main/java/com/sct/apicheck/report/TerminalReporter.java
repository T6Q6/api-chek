package com.sct.apicheck.report;

import com.sct.apicheck.assertion.Verdict;
import com.sct.apicheck.run.ApiResult;
import com.sct.apicheck.run.RunResult;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 终端巡检报告渲染（PRD §5.2 版式）：每接口一行 + 汇总。
 *
 * <p>渲染为字符串而不是直接打印，便于测试断言与调用方决定输出目标。
 * P0 范围只含「总接口 / 通过 / 失败 / 慢请求 / 成功率」，不含分位数与并发（PLAN §1.2）。</p>
 */
public class TerminalReporter {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    public String render(RunResult result) {
        StringBuilder report = new StringBuilder();
        report.append("API 健康巡检报告\n");
        report.append("Run ID: ").append(result.getRunId()).append('\n');
        report.append("时间: ").append(TIME.format(result.getStartTime())).append('\n');
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
