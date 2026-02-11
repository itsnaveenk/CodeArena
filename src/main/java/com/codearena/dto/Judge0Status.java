package com.codearena.dto;

public record Judge0Status(int id, String description) {
    public static final int IN_QUEUE = 1;
    public static final int PROCESSING = 2;
    public static final int ACCEPTED = 3;
    public static final int WRONG_ANSWER = 4;
    public static final int TIME_LIMIT_EXCEEDED = 5;
    public static final int COMPILATION_ERROR = 6;
    public static final int RUNTIME_ERROR_SIGSEGV = 7;
    public static final int RUNTIME_ERROR_SIGXFSZ = 8;
    public static final int RUNTIME_ERROR_SIGFPE = 9;
    public static final int RUNTIME_ERROR_SIGABRT = 10;
    public static final int RUNTIME_ERROR_NZEC = 11;
    public static final int RUNTIME_ERROR_OTHER = 12;
    public static final int INTERNAL_ERROR = 13;
    public static final int EXEC_FORMAT_ERROR = 14;

    public boolean isQueued() {
        return id == IN_QUEUE || id == PROCESSING;
    }

    public boolean isRuntimeError() {
        return id >= RUNTIME_ERROR_SIGSEGV && id <= RUNTIME_ERROR_OTHER;
    }
}
