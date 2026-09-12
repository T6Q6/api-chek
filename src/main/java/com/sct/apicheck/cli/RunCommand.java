package com.sct.apicheck.cli;

import com.sct.apicheck.assertion.AssertionEngine;
import com.sct.apicheck.config.ApiCheckConfig;
import com.sct.apicheck.config.ConfigException;
import com.sct.apicheck.config.ConfigLoader;
import com.sct.apicheck.config.ConfigValidator;
import com.sct.apicheck.config.ValidationResult;
import com.sct.apicheck.http.HttpCaller;
import com.sct.apicheck.report.ReportWriter;
import com.sct.apicheck.report.TerminalReporter;
import com.sct.apicheck.run.InspectionRunner;
import com.sct.apicheck.run.RunResult;
import com.sct.apicheck.trace.TraceWriter;
import com.sct.apicheck.trace.Tracer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.Callable;

/**
 * 顺序巡检并在终端输出报告，同时把 {@code report.json} 写入
 * {@code <output>/runs/<run_id>/}。
 *
 * <p>退出码：0 = 无 FAIL/ERROR；1 = 存在 FAIL/ERROR；2 = 配置/用法错误。</p>
 */
@Command(name = "run", description = "执行 API 巡检", mixinStandardHelpOptions = true)
public class RunCommand implements Callable<Integer> {

    private final ConfigLoader loader;
    private final ConfigValidator validator;
    private final HttpCaller caller;

    @Option(names = {"-c", "--config"}, required = true, description = "配置文件路径")
    private Path configPath;

    @Option(names = {"-o", "--output"}, defaultValue = ".api-check", description = "产物根目录")
    private Path outputDir;

    public RunCommand(ConfigLoader loader, ConfigValidator validator, HttpCaller caller) {
        this.loader = loader;
        this.validator = validator;
        this.caller = caller;
    }

    @Override
    public Integer call() {
        ApiCheckConfig config;
        try {
            config = loader.load(configPath);
        } catch (ConfigException e) {
            System.err.println("错误: " + e.getMessage());
            return 2;
        }

        ValidationResult validation = validator.validate(config);
        if (!validation.isValid()) {
            validation.getErrors().forEach(error -> System.err.println("错误: " + error));
            return 2;
        }

        Clock clock = Clock.systemUTC();
        Tracer tracer = new Tracer(clock);
        InspectionRunner runner = new InspectionRunner(caller,
                new AssertionEngine(config.getDefaults()), tracer, clock);
        RunResult result = runner.run(config);

        // {outputDir}/runs/{runId}/report.json + trace.jsonl
        Path runDir = outputDir.resolve("runs").resolve(result.getRunId());
        Path reportFile = runDir.resolve("report.json");
        Path traceFile = runDir.resolve("trace.jsonl");
        try {
            new ReportWriter().write(result, reportFile);
            new TraceWriter().write(tracer.spans(), traceFile);
        } catch (RuntimeException e) {
            System.err.println("错误: 写入产物失败: " + e.getMessage());
            return 1;
        }

        System.out.println(new TerminalReporter().render(result));
        System.out.println("报告已写入: " + reportFile);
        System.out.println("链路已写入: " + traceFile);
        // trace_id 与 run_id 不同名（trace_… / run_…），直接印出来省得用户拿 run_id 去 show。
        System.out.println("trace_id: " + tracer.traceId());
        return result.getFailed() > 0 ? 1 : 0;
    }
}
