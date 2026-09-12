package com.sct.apicheck.run;

import com.sct.apicheck.assertion.Verdict;

import java.time.Instant;
import java.util.List;

/**
 * 一次巡检的汇总结果。
 *
 * <p>计数口径对齐 PRD §8.2 的 {@code report.json}：{@code failed} 合并 FAIL 与 ERROR
 * （P0 不单列 ERROR 计数）；成功率把 SLOW 视为成功，即 {@code (passed + slow) / total}。</p>
 */
public class RunResult {

    private final String runId;
    private final Instant startTime;
    private final Instant endTime;
    private final List<ApiResult> results;

    public RunResult(String runId, Instant startTime, Instant endTime, List<ApiResult> results) {
        this.runId = runId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.results = results == null ? List.of() : List.copyOf(results);
    }

    public String getRunId() {
        return runId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public List<ApiResult> getResults() {
        return results;
    }

    public int getTotal() {
        return results.size();
    }

    public int getPassed() {
        return count(Verdict.PASS);
    }

    public int getFailed() {
        return count(Verdict.FAIL) + count(Verdict.ERROR);
    }

    public int getSlow() {
        return count(Verdict.SLOW);
    }

    public double getSuccessRate() {
        if (results.isEmpty()) {
            return 0.0;
        }
        return (double) (getPassed() + getSlow()) / getTotal();
    }

    private int count(Verdict verdict) {
        return (int) results.stream().filter(r -> r.getVerdict() == verdict).count();
    }
}
