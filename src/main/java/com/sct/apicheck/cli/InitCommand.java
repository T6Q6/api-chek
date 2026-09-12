package com.sct.apicheck.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sct.apicheck.config.ApiCheckConfig;
import com.sct.apicheck.config.ApiDefinition;
import com.sct.apicheck.config.AssertConfig;
import com.sct.apicheck.config.DefaultsConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 生成示例 apis.yaml。
 */
@Command(name = "init", description = "生成示例 apis.yaml", mixinStandardHelpOptions = true)
public class InitCommand implements Callable<Integer> {

    private final ObjectMapper yamlMapper;

    @Option(names = {"-o", "--output"}, defaultValue = "apis.yaml", description = "输出文件路径")
    private Path outputPath;

    public InitCommand(ObjectMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    @Override
    public Integer call() throws Exception {
        yamlMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), sampleConfig());
        System.out.println("示例配置已生成: " + outputPath);
        return 0;
    }

    static ApiCheckConfig sampleConfig() {
        DefaultsConfig defaults = new DefaultsConfig();
        defaults.setBaseUrl("https://api.example.com");
        defaults.setTimeoutMs(3000);
        defaults.setRetry(1);
        defaults.setSlowThresholdMs(800);
        defaults.setHeaders(Map.of("User-Agent", "api-check/1.0"));

        ApiDefinition getUser = new ApiDefinition();
        getUser.setName("获取用户详情");
        getUser.setMethod("GET");
        getUser.setPath("/api/users/1");
        getUser.setExpectedStatus(List.of(200));
        getUser.setMaxRtMs(500);
        getUser.setAssertions(assertions("$.code", 0, "$.data.id", 1, "$.data.name", "张三"));
        getUser.setTags(List.of("core", "user"));

        ApiDefinition createOrder = new ApiDefinition();
        createOrder.setName("创建订单");
        createOrder.setMethod("POST");
        createOrder.setPath("/api/orders");
        createOrder.setHeaders(Map.of("Content-Type", "application/json"));
        createOrder.setBody(body());
        createOrder.setExpectedStatus(List.of(200, 201));
        createOrder.setMaxRtMs(1000);
        createOrder.setAssertions(assertions("$.code", 0, "$.data.orderId", "not_null"));
        createOrder.setTags(List.of("core", "order"));

        ApiCheckConfig config = new ApiCheckConfig();
        config.setVersion(1);
        config.setDefaults(defaults);
        config.setApis(List.of(getUser, createOrder));
        return config;
    }

    private static AssertConfig assertions(Object... pairs) {
        Map<String, Object> jsonPath = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            jsonPath.put((String) pairs[i], pairs[i + 1]);
        }
        AssertConfig assertions = new AssertConfig();
        assertions.setJsonPath(jsonPath);
        return assertions;
    }

    private static JsonNode body() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("userId", 1);
        node.put("productId", 100);
        node.put("count", 2);
        return node;
    }
}