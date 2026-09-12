package com.sct.apicheck.cli;

import com.sct.apicheck.config.ApiCheckConfig;
import com.sct.apicheck.config.ConfigException;
import com.sct.apicheck.config.ConfigLoader;
import com.sct.apicheck.config.ConfigValidator;
import com.sct.apicheck.config.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * 校验 apis.yaml 的合法性。
 */
@Command(name = "validate", description = "校验 API 清单配置", mixinStandardHelpOptions = true)
public class ValidateCommand implements Callable<Integer> {

    private final ConfigLoader loader;
    private final ConfigValidator validator;

    @Option(names = {"-c", "--config"}, required = true, description = "配置文件路径")
    private Path configPath;

    public ValidateCommand(ConfigLoader loader, ConfigValidator validator) {
        this.loader = loader;
        this.validator = validator;
    }

    @Override
    public Integer call() {
        try {
            ApiCheckConfig config = loader.load(configPath);
            ValidationResult result = validator.validate(config);
            if (result.isValid()) {
                System.out.println("配置合法: " + config.getApis().size() + " 个接口");
                return 0;
            }
            result.getErrors().forEach(e -> System.err.println("错误: " + e));
            return 2;
        } catch (ConfigException e) {
            System.err.println("错误: " + e.getMessage());
            return 2;
        }
    }
}