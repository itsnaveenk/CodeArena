package com.codearena.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codearena.dto.CreateTestcaseRequest;
import com.codearena.dto.TestcaseDto;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;
import com.codearena.service.TestcaseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class TestcaseController {

    private final TestcaseService testcaseService;

    public TestcaseController(TestcaseService testcaseService) {
        this.testcaseService = testcaseService;
    }

    @PostMapping("/problems/{problemId}/testcases")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> addTestcase(
            @PathVariable Long problemId,
            @Valid @RequestBody CreateTestcaseRequest request,
            @AuthenticationPrincipal User user) {
        Testcase testcase = testcaseService.addTestcase(problemId, request, user);
        TestcaseDto dto = new TestcaseDto(
            testcase.getId(),
            testcase.getInput(),
            testcase.getExpectedOutput(),
            testcase.isHidden()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/problems/{problemId}/testcases")
    public ResponseEntity<List<TestcaseDto>> getTestcases(
            @PathVariable Long problemId,
            @AuthenticationPrincipal User user) {
        List<TestcaseDto> testcases = testcaseService.getTestcases(problemId, user);
        return ResponseEntity.ok(testcases);
    }

    @DeleteMapping("/testcases/{testcaseId}")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTestcase(
            @PathVariable Long testcaseId,
            @AuthenticationPrincipal User user) {
        testcaseService.deleteTestcase(testcaseId, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/testcases/{testcaseId}")
    @PreAuthorize("hasRole('PROBLEM_SETTER') or hasRole('ADMIN')")
    public ResponseEntity<TestcaseDto> updateTestcase(
            @PathVariable Long testcaseId,
            @Valid @RequestBody CreateTestcaseRequest request,
            @AuthenticationPrincipal User user) {
        Testcase testcase = testcaseService.updateTestcase(testcaseId, request, user);
        return ResponseEntity.ok(TestcaseDto.fromEntity(testcase));
    }
}
