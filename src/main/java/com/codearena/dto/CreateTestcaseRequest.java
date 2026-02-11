package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTestcaseRequest(
    @NotBlank(message = "Input is required")
    String input,
    
    @NotBlank(message = "Expected output is required")
    String expectedOutput,
    
    boolean isHidden
) {
    public static CreateTestcaseRequest visible(String input, String expectedOutput) {
        return new CreateTestcaseRequest(input, expectedOutput, false);
    }

    public static CreateTestcaseRequest hidden(String input, String expectedOutput) {
        return new CreateTestcaseRequest(input, expectedOutput, true);
    }
}
