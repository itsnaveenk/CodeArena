package com.codearena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Judge0Request(
    @JsonProperty("language_id") int languageId,
    @JsonProperty("source_code") String sourceCode,
    String stdin,
    @JsonProperty("expected_output") String expectedOutput,
    @JsonProperty("cpu_time_limit") Double cpuTimeLimit,
    @JsonProperty("memory_limit") Integer memoryLimit
) {
    public Judge0Request(int languageId, String sourceCode, String stdin) {
        this(languageId, sourceCode, stdin, null, 5.0, 128000);
    }
}
