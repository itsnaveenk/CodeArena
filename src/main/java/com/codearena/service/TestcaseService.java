package com.codearena.service;

import java.util.List;

import com.codearena.dto.CreateTestcaseRequest;
import com.codearena.dto.TestcaseDto;
import com.codearena.entity.Testcase;
import com.codearena.entity.User;

public interface TestcaseService {

    Testcase addTestcase(Long problemId, CreateTestcaseRequest request, User creator);

    List<TestcaseDto> getTestcases(Long problemId, User requester);

    void deleteTestcase(Long testcaseId, User requester);

    List<Testcase> getAllTestcasesForExecution(Long problemId);

    List<Testcase> getVisibleTestcases(Long problemId);

    Testcase updateTestcase(Long testcaseId, CreateTestcaseRequest request, User requester);
}
