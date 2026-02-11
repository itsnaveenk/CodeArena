package com.codearena.service;

import com.codearena.dto.RunRequest;
import com.codearena.dto.RunResponse;
import com.codearena.dto.SubmissionResponse;
import com.codearena.dto.SubmitRequest;
import com.codearena.entity.User;

public interface ExecutionService {

    RunResponse runCode(RunRequest request, User user);

    SubmissionResponse submitCode(SubmitRequest request, User user);
}
