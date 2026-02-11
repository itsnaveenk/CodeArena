package com.codearena.execution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ProcessRunner {

    private ProcessRunner() {
    }

    static ProcessResult run(
            List<String> command,
            String stdin,
            java.nio.file.Path workingDirectory,
            Duration timeout,
            int maxOutputBytes) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDirectory.toFile());
        Process process = pb.start();

        if (stdin != null && !stdin.isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } else {
            process.getOutputStream().close();
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        Thread tOut = new Thread(() -> copyBounded(process.getInputStream(), stdout, maxOutputBytes), "codearena-proc-stdout");
        Thread tErr = new Thread(() -> copyBounded(process.getErrorStream(), stderr, maxOutputBytes), "codearena-proc-stderr");
        tOut.setDaemon(true);
        tErr.setDaemon(true);
        tOut.start();
        tErr.start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        }

        tOut.join(TimeUnit.SECONDS.toMillis(2));
        tErr.join(TimeUnit.SECONDS.toMillis(2));

        int exitCode = finished ? process.exitValue() : -1;

        return new ProcessResult(finished, exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private static void copyBounded(InputStream in, ByteArrayOutputStream out, int maxBytes) {
        byte[] buf = new byte[8192];
        int total = 0;
        boolean truncated = false;
        try (in) {
            int read;
            while ((read = in.read(buf)) != -1) {
                int remaining = maxBytes - total;
                if (remaining > 0) {
                    int toWrite = Math.min(read, remaining);
                    out.write(buf, 0, toWrite);
                    total += toWrite;
                } else {
                    truncated = true;
                }
            }
        } catch (IOException ignored) {
        }

        if (truncated) {
            try {
                out.write("\n[output truncated]\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
    }

    record ProcessResult(boolean finished, int exitCode, String stdout, String stderr) {
    }
}
