package com.sct.apicheck.assertion;

import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.http.CallResult;

import java.util.List;

/**
 * 单项检查器：负责一类「会导致 FAIL」的断言（状态码 / JSONPath / 文本包含）。
 *
 * <p>统一接口便于后续阶段扩展新断言类型（如正则）。RT 不在此列——RT 超阈值判定为
 * {@link Verdict#SLOW} 而非 FAIL，由 {@link AssertionEngine} 单独处理。</p>
 */
interface AssertionChecker {

    /**
     * 执行检查并将每条失败说明追加到 {@code failures}；通过时不写入任何内容。
     */
    void check(ApiDefinition api, CallResult result, List<String> failures);
}
