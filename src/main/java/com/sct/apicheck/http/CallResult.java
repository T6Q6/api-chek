package com.sct.apicheck.http;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;

/**
 * 单次 HTTP 调用的类型化结果。
 *
 * <p>{@code errorType == NONE} 表示成功拿到了响应（无论状态码是多少）；
 * 其余表示请求在传输层就失败了，此时 {@code statusCode} 为 0。</p>
 */
public class CallResult {

    public enum ErrorType {
        NONE, TIMEOUT, CONNECTION, DNS, TLS, IO, UNKNOWN;

        /**
         * 按异常类型归类。注意判断顺序：具体异常（均为 IOException 子类）必须排在 IOException 之前。
         */
        public static ErrorType from(Throwable t) {
            if (t == null) {
                return UNKNOWN;
            }
            if (t instanceof HttpTimeoutException) {
                return TIMEOUT;
            }
            if (t instanceof ConnectException) {
                return CONNECTION;
            }
            if (t instanceof UnknownHostException) {
                return DNS;
            }
            if (t instanceof javax.net.ssl.SSLException) {
                return TLS;
            }
            if (t instanceof IOException) {
                return IO;
            }
            return UNKNOWN;
        }
    }

    private final int statusCode;
    private final long latencyMs;
    private final String body;
    private final Map<String, List<String>> headers;
    private final ErrorType errorType;
    private final String errorMessage;

    private CallResult(int statusCode, long latencyMs, String body, Map<String, List<String>> headers,
                       ErrorType errorType, String errorMessage) {
        this.statusCode = statusCode;
        this.latencyMs = latencyMs;
        this.body = body;
        this.headers = headers == null ? Map.of() : headers;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public static CallResult response(int statusCode, long latencyMs, String body,
                                      Map<String, List<String>> headers) {
        return new CallResult(statusCode, latencyMs, body, headers, ErrorType.NONE, null);
    }

    public static CallResult failure(ErrorType errorType, String message, long latencyMs) {
        return new CallResult(0, latencyMs, null, Map.of(), errorType, message);
    }

    public boolean hasResponse() {
        return errorType == ErrorType.NONE;
    }

    public boolean isError() {
        return errorType != ErrorType.NONE;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getBody() {
        return body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}