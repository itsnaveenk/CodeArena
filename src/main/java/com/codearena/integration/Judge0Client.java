package com.codearena.integration;

import java.time.Duration;

import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Submission;

public interface Judge0Client {

    Judge0Submission createSubmission(Judge0Request request);

    Judge0Submission getSubmission(String token);

    Judge0Submission createAndWaitForResult(Judge0Request request, Duration timeout);
}
