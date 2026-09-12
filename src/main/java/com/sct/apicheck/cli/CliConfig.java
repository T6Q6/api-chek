package com.sct.apicheck.cli;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sct.apicheck.config.ConfigLoader;
import com.sct.apicheck.config.ConfigValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import picocli.CommandLine;

/**
 * CLI 命令层装配：领域对象（Loader/Validator）以纯 Java 实例注入，命令图用 Picocli 组装。
 */
@Configuration
public class CliConfig {

    @Bean
    public ObjectMapper yamlObjectMapper() {
        return new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Bean
    public ConfigLoader configLoader(ObjectMapper yamlObjectMapper) {
        return new ConfigLoader(yamlObjectMapper);
    }

    @Bean
    public ConfigValidator configValidator() {
        return new ConfigValidator();
    }

    @Bean
    public InitCommand initCommand(ObjectMapper yamlObjectMapper) {
        return new InitCommand(yamlObjectMapper);
    }

    @Bean
    public ValidateCommand validateCommand(ConfigLoader configLoader, ConfigValidator configValidator) {
        return new ValidateCommand(configLoader, configValidator);
    }

    @Bean
    public CommandLine commandLine(InitCommand initCommand, ValidateCommand validateCommand) {
        CommandLine commandLine = new CommandLine(new ApiCheckCommand());
        commandLine.addSubcommand(initCommand);
        commandLine.addSubcommand(validateCommand);
        return commandLine;
    }
}