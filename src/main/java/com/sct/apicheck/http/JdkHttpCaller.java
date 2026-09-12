package com.sct.apicheck.http;

import com.sct.apicheck.config.ApiDefinition;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 基于 JDK 内置 {@link HttpClient} 的实现，无额外运行时依赖。
 *
 * <p>重试语义：仅在传输层失败（超时/连接/DNS 等 IOException）时重试，
 * {@code retry} 表示「额外尝试次数」，即总尝试次数 = retry + 1。
 * 5xx 属于正常响应，不触发重试，交由断言层判定。</p>
 *
 * <p>超时语义：{@code timeoutMs} 是「整次交互」的墙钟上限，包含读取响应体。
 * 不能用 {@link HttpRequest.Builder#timeout} 单独实现——JDK 在一次 exchange 的
 * 响应头到达时即视为完成，其请求级计时器随之撤销，服务端「头秒回、体慢吐」时不会触发超时，
 * 请求会一直阻塞到 body 读完。</p>
 */
public class JdkHttpCaller implements HttpCaller {

    private static final long DEFAULT_TIMEOUT_MS = 5000;

    private final HttpClient client;

    public JdkHttpCaller() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    JdkHttpCaller(HttpClient client) {
        this.client = client;
    }

    @Override
    public CallResult call(ApiDefinition api, String baseUrl) {
        URI uri;
        try {
            uri = buildUri(baseUrl, api);
        } catch (RuntimeException e) {
            return CallResult.failure(CallResult.ErrorType.UNKNOWN, "URL 无效: " + e.getMessage(), 0);
        }

        long timeoutMs = api.getTimeoutMs() != null ? api.getTimeoutMs() : DEFAULT_TIMEOUT_MS;
        int retry = api.getRetry() != null ? Math.max(0, api.getRetry()) : 0;
        int attempts = retry + 1;

        long start = System.nanoTime();
        Throwable lastError = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                HttpResponse<String> response = send(buildRequest(api, uri, timeoutMs), timeoutMs);
                return CallResult.response(response.statusCode(), elapsedMs(start), response.body(),
                        response.headers().map());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CallResult.failure(CallResult.ErrorType.IO, "请求被中断", elapsedMs(start));
            } catch (IOException e) {
                lastError = e;
            } catch (RuntimeException e) {
                return CallResult.failure(CallResult.ErrorType.from(e), describe(e), elapsedMs(start));
            }
        }
        return CallResult.failure(CallResult.ErrorType.from(lastError), describe(lastError), elapsedMs(start));
    }

    /**
     * 给整次交互（连接 + 响应头 + 响应体）加墙钟上限。超时抛 {@link HttpTimeoutException}
     * （属于 IOException），因此会走调用方的重试分支，与传输层失败语义一致。
     */
    private HttpResponse<String> send(HttpRequest request, long timeoutMs)
            throws IOException, InterruptedException {
        CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new HttpTimeoutException("请求超时（含响应体读取）: " + timeoutMs + "ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IOException(cause);
        }
    }

    private static HttpRequest buildRequest(ApiDefinition api, URI uri, long timeoutMs) {
        String method = (api.getMethod() == null || api.getMethod().isBlank())
                ? "GET"
                : api.getMethod().trim().toUpperCase(Locale.ROOT);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(timeoutMs));
        Map<String, String> headers = api.getHeaders();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null) {
                    builder.header(name, value);
                }
            });
        }

        if (api.getBody() != null && !api.getBody().isNull()) {
            if (!containsHeaderIgnoreCase(headers, "Content-Type")) {
                builder.header("Content-Type", "application/json");
            }
            builder.method(method,
                    HttpRequest.BodyPublishers.ofString(api.getBody().toString(), StandardCharsets.UTF_8));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    private static URI buildUri(String baseUrl, ApiDefinition api) {
        String full;
        if (api.getUrl() != null && !api.getUrl().isBlank()) {
            full = api.getUrl().trim();
        } else {
            full = joinUrl(baseUrl == null ? "" : baseUrl.trim(),
                    api.getPath() == null ? "" : api.getPath().trim());
        }
        String query = encodeParams(api.getParams());
        if (!query.isEmpty()) {
            full = full + (full.contains("?") ? "&" : "?") + query;
        }
        return URI.create(full);
    }

    private static String joinUrl(String base, String path) {
        if (base.isEmpty()) {
            return path;
        }
        if (path.isEmpty()) {
            return base;
        }
        boolean baseSlash = base.endsWith("/");
        boolean pathSlash = path.startsWith("/");
        if (baseSlash && pathSlash) {
            return base + path.substring(1);
        }
        if (!baseSlash && !pathSlash) {
            return base + "/" + path;
        }
        return base + path;
    }

    private static String encodeParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .map(e -> urlEncode(e.getKey()) + "=" + urlEncode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static boolean containsHeaderIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null) {
            return false;
        }
        return headers.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(name));
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String describe(Throwable t) {
        if (t == null) {
            return "未知错误";
        }
        String message = t.getMessage();
        return t.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}