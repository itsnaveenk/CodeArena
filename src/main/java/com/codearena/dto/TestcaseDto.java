package com.codearena.dto;

import com.codearena.entity.Testcase;

public record TestcaseDto(Long id, String input, String expectedOutput, boolean isHidden) {
    public static TestcaseDto fromEntity(Testcase testcase) {
        return new TestcaseDto(testcase.getId(), testcase.getInput(), testcase.getExpectedOutput(), testcase.isHidden());
    }

    public static TestcaseDto fromEntityHidden(Testcase testcase) {
        return new TestcaseDto(testcase.getId(), null, null, testcase.isHidden());
    }
}
