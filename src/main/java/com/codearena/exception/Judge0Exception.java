package com.codearena.exception;

public class Judge0Exception extends RuntimeException {

    private final boolean timeout;

    public Judge0Exception(String message) {
        super(message);
        this.timeout = false;
    }

    public Judge0Exception(String message, boolean timeout) {
        super(message);
        this.timeout = timeout;
    }

    public Judge0Exception(String message, Throwable cause) {
        super(message, cause);
        this.timeout = false;
    }

    public Judge0Exception(String message, Throwable cause, boolean timeout) {
        super(message, cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
