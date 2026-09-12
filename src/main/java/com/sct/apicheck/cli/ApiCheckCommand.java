package com.sct.apicheck.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * 根命令 api-check。
 */
@Command(name = "api-check",
        description = "API 健康巡检 CLI 工具",
        mixinStandardHelpOptions = true,
        version = "api-check 0.1.0")
public class ApiCheckCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}