package com.sct.apicheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

@SpringBootApplication
public class ApiChekApplication {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(ApiChekApplication.class)) {
            int exitCode = context.getBean(CommandLine.class).execute(args);
            System.exit(exitCode);
        }
    }
}