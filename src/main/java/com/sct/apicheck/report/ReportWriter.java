package com.sct.apicheck.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sct.apicheck.run.ApiResult;
import com.sct.apicheck.run.RunResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 将 {@link RunResult} 落盘为 {@code report.json}（结构对齐 PRD §8.2）。
 *
 * <p>显式构造 JSON 树而非直接序列化领域对象：一是只输出报告的派生视图（不泄露 api/call/assertion 内部结构），
 * 二是 {@code success_rate} 需要按 PRD 保留三位小数，而 {@link RunResult#getSuccessRate()} 保持全精度供终端使用。</p>
 */
public class ReportWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void write(RunResult result, Path file) {
        ObjectNode report = toJson(result);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), report);
        } catch (IOException e) {
            throw new UncheckedIOException("写入报告失败: " + file, e);
        }
    }

    private static ObjectNode toJson(RunResult result) {
        ObjectNode report = MAPPER.createObjectNode();
        report.put("run_id", result.getRunId());
        report.put("start_time", formatTimestamp(result.getStartTime()));
        report.put("end_time", formatTimestamp(result.getEndTime()));
        report.put("total", result.getTotal());
        report.put("passed", result.getPassed());
        report.put("failed", result.getFailed());
        report.put("slow", result.getSlow());
        report.put("success_rate", roundToThreeDecimals(result.getSuccessRate()));

        ArrayNode results = report.putArray("results");
        for (ApiResult api : result.getResults()) {
            results.add(toJson(api));
        }
        return report;
    }

    private static ObjectNode toJson(ApiResult api) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", api.getName());
        node.put("method", api.getMethod());
        node.put("path", api.getPath());
        node.put("status_code", api.getStatusCode());
        node.put("rt_ms", api.getLatencyMs());
        node.put("verdict", api.getVerdict().name());

        ArrayNode messages = node.putArray("messages");
        for (String message : api.getMessages()) {
            messages.add(message);
        }
        return node;
    }

    /**
     * 时间戳只保留到秒。{@code Clock.systemUTC().instant()} 带纳秒尾数，直接输出会得到
     * {@code 2026-09-12T10:46:58.060276100Z} 这种噪声；截断（而非四舍五入）到秒即可，
     * 精度损失无实际意义 —— 每个接口的耗时另有 {@code rt_ms} 记录。
     */
    private static String formatTimestamp(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    private static double roundToThreeDecimals(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
