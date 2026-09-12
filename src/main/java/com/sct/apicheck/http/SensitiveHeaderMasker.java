package com.sct.apicheck.http;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 把请求头里的敏感值打码，供日志/链路追踪展示，避免 Authorization、Cookie、Token 泄露。
 */
public final class SensitiveHeaderMasker {

    public static final String MASK = "******";

    private static final Set<String> SENSITIVE_NAMES = Set.of(
            "authorization", "proxy-authorization", "cookie", "set-cookie");

    private SensitiveHeaderMasker() {
    }

    public static boolean isSensitive(String headerName) {
        if (headerName == null) {
            return false;
        }
        String normalized = headerName.trim().toLowerCase(Locale.ROOT);
        return SENSITIVE_NAMES.contains(normalized) || normalized.contains("token");
    }

    /**
     * 返回打码后的副本，不修改传入的 map。
     */
    public static Map<String, String> mask(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        if (headers == null) {
            return masked;
        }
        headers.forEach((name, value) -> masked.put(name, isSensitive(name) ? MASK : value));
        return masked;
    }
}