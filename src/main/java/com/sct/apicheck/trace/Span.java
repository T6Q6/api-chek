package com.sct.apicheck.trace;

import java.time.Instant;
import java.util.Map;

/**
 * 一个 Span，字段对齐 PRD §5.5。
 *
 * <p>刻意做成可变对象而不是 record：Span 是「边执行边填」的 —— {@link Tracer} 进入时只填
 * start_time，执行完才知道 end_time/duration_ms，而 status/input/output 由被埋点的那段业务代码
 * 在回调里补写。用 record 就得反复构造新对象，反而绕。</p>
 *
 * <p>{@code parentId} 为 {@code null} 表示根 Span；{@code tokenUsage} 是 Phase 6（大模型分析）才填的字段，
 * Phase 5 恒为 {@code null}，但保留在结构里以保证 {@code trace.jsonl} 的形状从 Phase 5 起就稳定。</p>
 */
public class Span {

    private String traceId;
    private String spanId;
    private String parentId;
    private SpanType type;
    private String apiName;
    private Instant startTime;
    private Instant endTime;
    private long durationMs;
    private String status;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private Integer tokenUsage;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public SpanType getType() {
        return type;
    }

    public void setType(SpanType type) {
        this.type = type;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    public Integer getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(Integer tokenUsage) {
        this.tokenUsage = tokenUsage;
    }
}
