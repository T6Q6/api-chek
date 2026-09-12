package com.sct.apicheck.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把 Span 落盘为 {@code trace.jsonl}（每行一个 Span，PRD §8.3）。
 *
 * <p>与 {@link com.sct.apicheck.report.ReportWriter} 同一套路：显式构造 JSON 树，只输出 Span 这个对外契约，
 * 而不是把领域对象整包序列化出去。</p>
 */
public class TraceWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 全量重写：同一文件上的既有内容整体让位。Span 在巡检过程中攒在内存里、跑完一次落盘时用它。
     *
     * <p>与 {@link #append(Span, Path)} 的「追加」是**两种显式语义**，不合成一个含糊的「保存」——
     * 「重跑时旧内容要不要留」这件事，调用方必须有意识地选一次。</p>
     */
    public void write(List<Span> spans, Path file) {
        List<String> lines = new ArrayList<>();
        for (Span span : spans) {
            lines.add(toJson(span).toString());
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写入链路失败: " + file, e);
        }
    }

    /**
     * 增量追加：一条 Span 落一行，已写下去的行不动。逐条产出 Span 时用它 ——
     * 巡检中断也不会把已经完成的 trace 丢掉（PLAN §7「追加写」、PRD §9.2.5）。
     *
     * <p>{@code CREATE} 让文件不存在时顺手建出来，与 {@code write} 一样负责补父目录，
     * 调用方不必先探一次路径。</p>
     */
    public void append(Span span, Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, toJson(span).toString() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("追加链路失败: " + file, e);
        }
    }

    private static ObjectNode toJson(Span span) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("trace_id", span.getTraceId());
        node.put("span_id", span.getSpanId());
        putText(node, "parent_id", span.getParentId());
        putText(node, "name", span.getType() == null ? null : span.getType().wireName());
        putText(node, "api_name", span.getApiName());
        putText(node, "start_time", formatInstant(span.getStartTime()));
        putText(node, "end_time", formatInstant(span.getEndTime()));
        node.put("duration_ms", span.getDurationMs());
        putText(node, "status", span.getStatus());
        putMap(node, "input", span.getInput());
        putMap(node, "output", span.getOutput());
        if (span.getTokenUsage() == null) {
            node.putNull("token_usage");
        } else {
            node.put("token_usage", span.getTokenUsage());
        }
        return node;
    }

    /**
     * 时间为空也要落成显式 {@code null}（而不是整个字段消失）：读端与消费方可以无条件按字段名取值，
     * 不必先判 {@code has(...)}。
     */
    private static void putText(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private static void putMap(ObjectNode node, String field, Map<String, Object> value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.set(field, MAPPER.valueToTree(value));
        }
    }

    /**
     * 时间是给人读的，保持 UTC ISO 形式；毫秒足够，纳秒是噪声。
     */
    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.MILLIS));
    }
}
