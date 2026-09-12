package com.sct.apicheck.trace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 读回 {@code trace.jsonl}（{@code trace show} 需要）。与 {@link TraceWriter} 共用同一套字段约定，
 * 拆成两个类而不是让 Writer 兼任读：写和读是两个独立的契约，各自有独立的测试。
 *
 * <p>刻意逐字段手工映射，而不是 {@code readValue(line, Span.class)}：落盘字段名（{@code name}）与领域属性
 * （{@code type}）不同名，且 {@code name} 是 wireName 需要经 {@link SpanType#tryFromWire} 翻译。手工映射把
 * 「文件格式 ↔ 对象」的翻译收在一个地方，将来字段改名不会悄悄改变协议。</p>
 */
public class TraceReader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public List<Span> read(Path file) {
        List<Span> spans = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                Span span = toSpan(MAPPER.readTree(line));
                if (span != null) {
                    spans.add(span);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("读取链路失败: " + file, e);
        }
        return spans;
    }

    /**
     * 认不得的 Span 类型返回 {@code null} 交给调用方跳过：落盘词汇表比实现宽（PRD §5.5），
     * 一条将来才有的 {@code llm_analysis}/{@code alert} 不该让整份 trace 读不出来。
     */
    private static Span toSpan(JsonNode node) {
        String name = text(node, "name");
        SpanType type = name == null ? null : SpanType.tryFromWire(name);
        if (name != null && type == null) {
            return null;
        }

        Span span = new Span();
        span.setTraceId(text(node, "trace_id"));
        span.setSpanId(text(node, "span_id"));
        span.setParentId(text(node, "parent_id"));
        span.setType(type);

        span.setApiName(text(node, "api_name"));
        span.setStartTime(instant(node, "start_time"));
        span.setEndTime(instant(node, "end_time"));

        JsonNode duration = node.get("duration_ms");
        if (duration != null && !duration.isNull()) {
            span.setDurationMs(duration.asLong());
        }

        span.setStatus(text(node, "status"));
        span.setInput(objectMap(node, "input"));
        span.setOutput(objectMap(node, "output"));

        JsonNode tokens = node.get("token_usage");
        if (tokens != null && !tokens.isNull()) {
            span.setTokenUsage(tokens.asInt());
        }
        return span;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : Instant.parse(value);
    }

    private static Map<String, Object> objectMap(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return MAPPER.convertValue(value, new TypeReference<Map<String, Object>>() {});
    }
}
