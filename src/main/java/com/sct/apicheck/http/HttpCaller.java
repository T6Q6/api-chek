package com.sct.apicheck.http;

import com.sct.apicheck.config.ApiDefinition;

/**
 * 发起一次 HTTP 调用并返回类型化结果，不抛异常——传输层失败也通过 {@link CallResult} 表达。
 */
public interface HttpCaller {

    /**
     * @param api     接口定义；若 {@code url} 为空则用 {@code baseUrl + path} 拼接
     * @param baseUrl 全局默认根地址，可为 null（当 api 自带完整 url 时）
     */
    CallResult call(ApiDefinition api, String baseUrl);
}