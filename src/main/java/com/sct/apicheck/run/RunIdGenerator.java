package com.sct.apicheck.run;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生成巡检 run id：{@code run_yyyyMMdd_HHmmss}（UTC，与 report.json 的 {@code Z} 时区一致）。
 *
 * <p>同一秒内重复调用时追加 {@code _2}、{@code _3} … 以去重。</p>
 */
public class RunIdGenerator {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    private final Clock clock;
    private final Map<String, Integer> issued = new ConcurrentHashMap<>();

    public RunIdGenerator(Clock clock) {
        this.clock = clock;
    }

    public String next() {
        String base = "run_" + FORMAT.format(clock.instant());
        int sequence = issued.merge(base, 1, Integer::sum);
        return sequence == 1 ? base : base + "_" + sequence;
    }
}
