package com.codearena.execution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codearena.config.ExecutionConfig;
import com.codearena.dto.Judge0Status;
import com.codearena.dto.Judge0Submission;
import com.codearena.entity.SupportedLanguage;

@Component
public class LocalToolchainExecutor {

    private static final Logger log = LoggerFactory.getLogger(LocalToolchainExecutor.class);

    private final ExecutionConfig config;

    public LocalToolchainExecutor(ExecutionConfig config) {
        this.config = config;
    }

    public Judge0Submission execute(int languageId, String sourceCode, String stdin, Duration timeout) {
        SupportedLanguage language;
        try {
            language = SupportedLanguage.fromJudge0Id(languageId);
        } catch (IllegalArgumentException e) {
            return runtimeError("Unsupported language ID: " + languageId, null);
        }

        Duration effectiveTimeout = (timeout != null) ? timeout : Duration.ofMillis(config.getLocal().getTimeoutMs());
        int maxOutputBytes = config.getLocal().getMaxOutputBytes();

        Path workDir = null;
        Instant start = Instant.now();
        try {
            workDir = Files.createTempDirectory("codearena-local-exec-");
            return switch (language) {
                case JAVA -> executeJava(sourceCode, stdin, workDir, effectiveTimeout, maxOutputBytes);
                case PYTHON -> executePython(sourceCode, stdin, workDir, effectiveTimeout, maxOutputBytes);
                case CPP -> executeCpp(sourceCode, stdin, workDir, effectiveTimeout, maxOutputBytes);
                default -> runtimeError("Local execution not supported for: " + language.getDisplayName(), secondsSince(start));
            };
        } catch (IOException e) {
            log.error("Local execution failed (I/O): {}", e.getMessage());
            return runtimeError("Local execution I/O error: " + e.getMessage(), secondsSince(start));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return timeLimitExceeded(secondsSince(start));
        } catch (RuntimeException e) {
            log.error("Local execution failed: {}", e.getMessage(), e);
            return runtimeError("Local execution error: " + e.getMessage(), secondsSince(start));
        } finally {
            if (workDir != null) deleteRecursively(workDir);
        }
    }

    private Judge0Submission executeJava(String sourceCode, String stdin, Path workDir, Duration timeout, int maxOutputBytes) throws IOException, InterruptedException {
        Path sourceFile = workDir.resolve("Main.java");
        Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
        Duration compileTimeout = timeout.multipliedBy(3).dividedBy(10);
        ProcessRunner.ProcessResult compile = ProcessRunner.run(List.of("javac", "-encoding", "UTF-8", "Main.java"), null, workDir, compileTimeout, maxOutputBytes);
        if (!compile.finished()) return timeLimitExceeded(null);
        if (compile.exitCode() != 0) return compilationError(compile.stderr());
        List<String> runCmd = List.of("java", "-Xmx" + config.getLocal().getJava().getXmxMb() + "m", "-cp", workDir.toString(), "Main");
        Instant start = Instant.now();
        ProcessRunner.ProcessResult run = ProcessRunner.run(runCmd, stdin, workDir, timeout, maxOutputBytes);
        Double time = secondsSince(start);
        if (!run.finished()) return timeLimitExceeded(time);
        if (run.exitCode() != 0) return runtimeError(run.stderr(), time);
        return accepted(run.stdout(), time);
    }

    private Judge0Submission executePython(String sourceCode, String stdin, Path workDir, Duration timeout, int maxOutputBytes) throws IOException, InterruptedException {
        Path sourceFile = workDir.resolve("main.py");
        Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
        Instant start = Instant.now();
        ProcessRunner.ProcessResult run = ProcessRunner.run(List.of("python3", "main.py"), stdin, workDir, timeout, maxOutputBytes);
        Double time = secondsSince(start);
        if (!run.finished()) return timeLimitExceeded(time);
        if (run.exitCode() != 0) return runtimeError(run.stderr(), time);
        return accepted(run.stdout(), time);
    }

    private Judge0Submission executeCpp(String sourceCode, String stdin, Path workDir, Duration timeout, int maxOutputBytes) throws IOException, InterruptedException {
        Path sourceFile = workDir.resolve("main.cpp");
        Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
        Duration compileTimeout = timeout.multipliedBy(3).dividedBy(10);
        Path executable = workDir.resolve("main");
        ProcessRunner.ProcessResult compile = ProcessRunner.run(List.of("g++", "-O2", "-std=c++17", "main.cpp", "-o", "main"), null, workDir, compileTimeout, maxOutputBytes);
        if (!compile.finished()) return timeLimitExceeded(null);
        if (compile.exitCode() != 0) return compilationError(compile.stderr());
        if (Files.exists(executable)) executable.toFile().setExecutable(true);
        Instant start = Instant.now();
        ProcessRunner.ProcessResult run = ProcessRunner.run(List.of("./main"), stdin, workDir, timeout, maxOutputBytes);
        Double time = secondsSince(start);
        if (!run.finished()) return timeLimitExceeded(time);
        if (run.exitCode() != 0) return runtimeError(run.stderr(), time);
        return accepted(run.stdout(), time);
    }

    private static void deleteRecursively(Path root) {
        try {
            if (!Files.exists(root)) return;
            try (var stream = Files.walk(root)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        } catch (IOException ignored) {}
    }

    private static Double secondsSince(Instant start) { return (double) Duration.between(start, Instant.now()).toMillis() / 1000.0; }

    private static Judge0Submission accepted(String stdout, Double time) { return new Judge0Submission(null, stdout, null, null, null, new Judge0Status(Judge0Status.ACCEPTED, "Accepted"), time, null); }
    private static Judge0Submission compilationError(String compileOutput) { return new Judge0Submission(null, null, null, compileOutput, null, new Judge0Status(Judge0Status.COMPILATION_ERROR, "Compilation Error"), null, null); }
    private static Judge0Submission timeLimitExceeded(Double time) { return new Judge0Submission(null, null, null, null, null, new Judge0Status(Judge0Status.TIME_LIMIT_EXCEEDED, "Time Limit Exceeded"), time, null); }
    private static Judge0Submission runtimeError(String stderr, Double time) { return new Judge0Submission(null, null, stderr, null, null, new Judge0Status(Judge0Status.RUNTIME_ERROR_OTHER, "Runtime Error"), time, null); }
}
