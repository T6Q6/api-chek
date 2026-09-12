package com.sct.apicheck.cli;

import com.sct.apicheck.trace.Span;
import com.sct.apicheck.trace.TraceReader;
import com.sct.apicheck.trace.TraceView;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * {@code api-check trace list} / {@code api-check trace show <trace_id>}（PRD §5.5、§6）。
 *
 * <p>父命令只持有 {@code -o}（产物目录），两个子命令通过 {@link ParentCommand} 取用；
 * {@code ScopeType.INHERIT} 让 {@code -o} 前后都能写，也会出现在子命令的 {@code --help} 里。</p>
 *
 * <p>定位 trace 一律靠「扫 {@code -o} 下的 trace.jsonl + 读首行 trace_id」，不从目录名反推 ——
 * 目录命名规则将来怎么变都不影响命令；{@code -o} 给到哪一层都认，见 {@link #traceFiles(Path)}。</p>
 */
@Command(name = "trace", description = "查看链路追踪",
        mixinStandardHelpOptions = true,
        subcommands = {TraceCommand.ListSubcommand.class, TraceCommand.ShowSubcommand.class})
public class TraceCommand implements Runnable {

    /**
     * {@code INHERIT} 让 {@code -o} 出现在子命令的 {@code --help} 里，也能写在子命令之后
     * （{@code trace show -o DIR <id>}）。否则「怎么指定产物目录」这件事在 {@code show --help}
     * 里只字不提 —— 用户漏掉 -o 时，看到的就是「未找到 trace」。
     */
    @Option(names = {"-o", "--output"}, defaultValue = ".api-check",
            description = "产物目录（给产物根 / runs / 某个 run 目录 / trace.jsonl 都能找到）",
            scope = CommandLine.ScopeType.INHERIT)
    Path outputDir;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    /** 向下找 trace.jsonl 的层数上限：{@code <root>/runs/<run_id>/trace.jsonl} 恰好第 3 层。 */
    private static final int MAX_DEPTH = 3;

    private static final String TRACE_FILE = "trace.jsonl";

    /**
     * 从 {@code -o} 指的位置往下找所有 {@code trace.jsonl}。
     *
     * <p>刻意**不假设**用户给的是哪一层。规范布局是 {@code <root>/runs/<run_id>/trace.jsonl}，但用户
     * 并不知道这件事，顺手给出的很可能是 trace 文件所在的那一层 —— 某个 run 目录、{@code runs} 目录，
     * 甚至文件本身。以前这里固定拼一个 {@code /runs}，于是「给对了附近、给错了层级」就查不到东西，
     * 而报错只说「未找到 trace」，用户根本看不出是层级不对。</p>
     *
     * <p>所以改成：给文件就用文件，给目录就往下搜（最多 {@link #MAX_DEPTH} 层）。定位始终靠
     * 「读首行的 trace_id」，不从目录名反推，目录命名规则将来怎么变都不影响命令。</p>
     *
     * <p>路径不存在（一次都没跑过）视为「没有 trace」，返回空列表而不是报错 —— {@code list}
     * 在空仓库里也应成功退出。</p>
     */
    static List<Path> traceFiles(Path outputDir) {
        if (Files.isRegularFile(outputDir)) {
            return TRACE_FILE.equals(outputDir.getFileName().toString())
                    ? List.of(outputDir)
                    : List.of();
        }
        if (!Files.isDirectory(outputDir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(outputDir, MAX_DEPTH)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(file -> TRACE_FILE.equals(file.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    @Command(name = "list", description = "列出已有 trace", mixinStandardHelpOptions = true)
    public static class ListSubcommand implements Callable<Integer> {

        @ParentCommand
        TraceCommand parent;

        @Override
        public Integer call() {
            for (Path file : TraceCommand.traceFiles(parent.outputDir)) {
                List<Span> spans = new TraceReader().read(file);
                if (!spans.isEmpty()) {
                    System.out.println(spans.get(0).getTraceId());
                }
            }
            return 0;
        }
    }

    @Command(name = "show", description = "查看某条 trace 的时间线", mixinStandardHelpOptions = true)
    public static class ShowSubcommand implements Callable<Integer> {

        @ParentCommand
        TraceCommand parent;

        @Parameters(index = "0", paramLabel = "<trace_id>", description = "trace id")
        String traceId;

        @Override
        public Integer call() {
            for (Path file : TraceCommand.traceFiles(parent.outputDir)) {
                List<Span> spans = new TraceReader().read(file);
                if (!spans.isEmpty() && traceId.equals(spans.get(0).getTraceId())) {
                    System.out.print(new TraceView().render(spans));
                    return 0;
                }
            }
            System.err.println("未找到 trace: " + traceId
                    + "（已扫描: " + parent.outputDir.toAbsolutePath() + " 及其下 " + MAX_DEPTH + " 层）");
            return 2;
        }
    }
}
