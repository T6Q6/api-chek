package com.sct.apicheck.assertion;

/**
 * 单个接口的巡检判定结果，对齐 PRD §5.3 判定表。
 *
 * <p>优先级（高到低）：{@link #ERROR} &gt; {@link #FAIL} &gt; {@link #SLOW} &gt; {@link #PASS}。
 * 即请求在传输层失败时一律为 ERROR，其次状态码/断言失败为 FAIL，
 * 都通过但 RT 超阈值为 SLOW。</p>
 */
public enum Verdict {
    PASS,
    FAIL,
    SLOW,
    ERROR
}
