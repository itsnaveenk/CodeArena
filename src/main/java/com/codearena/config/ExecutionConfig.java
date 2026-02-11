package com.codearena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties(prefix = "execution")
@Validated
public class ExecutionConfig {

    @NotBlank
    private String backend = "judge0";

    private boolean fallbackToLocal = true;

    private final Local local = new Local();

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public boolean isFallbackToLocal() {
        return fallbackToLocal;
    }

    public void setFallbackToLocal(boolean fallbackToLocal) {
        this.fallbackToLocal = fallbackToLocal;
    }

    public Local getLocal() {
        return local;
    }

    public static class Local {

        @Min(1)
        private int maxOutputBytes = 200_000;

        @Min(1)
        private int timeoutMs = 30_000;

        private final Java java = new Java();

        public int getMaxOutputBytes() {
            return maxOutputBytes;
        }

        public void setMaxOutputBytes(int maxOutputBytes) {
            this.maxOutputBytes = maxOutputBytes;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Java getJava() {
            return java;
        }

        public static class Java {

            @Min(16)
            private int xmxMb = 256;

            public int getXmxMb() {
                return xmxMb;
            }

            public void setXmxMb(int xmxMb) {
                this.xmxMb = xmxMb;
            }
        }
    }
}
