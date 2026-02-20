package com.codearena.entity;

import java.util.Arrays;

public enum SupportedLanguage {
    JAVA(62, "Java (OpenJDK 13.0.1)"),
    PYTHON(71, "Python (3.8.1)"),
    CPP(54, "C++ (GCC 9.2.0)"),
    JAVASCRIPT(63, "JavaScript (Node.js 12.14.0)"),
    C(4, "C (GCC 8.3.0)"),
    TYPESCRIPT(74, "TypeScript (3.7.4)"),
    GO(60, "Go (1.13.5)"),
    RUST(73, "Rust (1.40.0)");

    private final int judge0Id;
    private final String displayName;

    SupportedLanguage(int judge0Id, String displayName) {
        this.judge0Id = judge0Id;
        this.displayName = displayName;
    }

    public int getJudge0Id() {
        return judge0Id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SupportedLanguage fromJudge0Id(int id) {
        return Arrays.stream(values())
            .filter(l -> l.judge0Id == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported language ID: " + id));
    }
}
