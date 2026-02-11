package com.codearena.integration;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.codearena.config.Judge0Config;
import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Submission;
import com.codearena.exception.Judge0Exception;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Judge0ClientImpl implements Judge0Client {

    private static final Logger log = LoggerFactory.getLogger(Judge0ClientImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final Judge0Config config;

    public Judge0ClientImpl(RestTemplate judge0RestTemplate, Judge0Config config) {
        this.restTemplate = judge0RestTemplate;
        this.config = config;
    }

    @Override
    public Judge0Submission createSubmission(Judge0Request request) {
        validateJudge0Request(request);
        String url = config.getApiUrl() + "/submissions?base64_encoded=false&wait=true";

        HttpHeaders headers = createHeaders();
        Map<String, Object> payload = buildPayload(request);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(payload);
            log.info("Judge0 request URL: {}", url);
            log.info("Judge0 request body: {}", jsonBody);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize payload for logging", e);
        }
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Judge0Submission> response = restTemplate.exchange(url, HttpMethod.POST, entity, Judge0Submission.class);
            if (response.getBody() == null) {
                log.error("Judge0 returned null response body");
                throw new Judge0Exception("Code execution service returned empty response");
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Judge0 service unavailable: {}", e.getMessage());
            throw new Judge0Exception("Code execution service is temporarily unavailable", e);
        } catch (HttpServerErrorException e) {
            log.error("Judge0 server error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new Judge0Exception("Code execution service encountered an error", e);
        } catch (HttpClientErrorException e) {
            log.error("Judge0 client error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new Judge0Exception("Invalid request to code execution service: " + e.getMessage(), e);
        }
    }

    @Override
    public Judge0Submission getSubmission(String token) {
        String url = config.getApiUrl() + "/submissions/" + token + "?base64_encoded=false";
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Judge0Submission> response = restTemplate.exchange(url, HttpMethod.GET, entity, Judge0Submission.class);
            if (response.getBody() == null) {
                log.error("Judge0 returned null response for token: {}", token);
                throw new Judge0Exception("Code execution service returned empty response");
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.error("Judge0 service unavailable while polling: {}", e.getMessage());
            throw new Judge0Exception("Code execution service is temporarily unavailable", e);
        } catch (HttpServerErrorException e) {
            log.error("Judge0 server error while polling: {} - {}", e.getStatusCode(), e.getMessage());
            throw new Judge0Exception("Code execution service encountered an error", e);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Judge0 submission not found: {}", token);
            throw new Judge0Exception("Submission not found: " + token, e);
        } catch (HttpClientErrorException e) {
            log.error("Judge0 client error while polling: {} - {}", e.getStatusCode(), e.getMessage());
            throw new Judge0Exception("Error retrieving submission result", e);
        }
    }

    @Override
    public Judge0Submission createAndWaitForResult(Judge0Request request, Duration timeout) {
        validateJudge0Request(request);
        Judge0Submission submission = createSubmission(request);

        if (submission.token() == null) {
            log.error("Judge0 did not return a token");
            throw new Judge0Exception("Code execution service did not return a submission token");
        }

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeout.toMillis();
        int pollInterval = config.getPollInterval();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Judge0Submission result = getSubmission(submission.token());
            if (result.isCompleted()) {
                logCompletedSubmission(result);
                return result;
            }
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Polling interrupted for submission: {}", submission.token());
                throw new Judge0Exception("Code execution was interrupted", e);
            }
        }

        log.error("Judge0 submission timed out after {}ms for token: {}", timeoutMillis, submission.token());
        throw new Judge0Exception("Code execution timed out", true);
    }

    private void validateJudge0Request(Judge0Request request) {
        if (request == null) throw new IllegalArgumentException("Judge0 request must not be null");
        if (request.languageId() <= 0) throw new IllegalArgumentException("Judge0 language_id must be a positive integer");
        if (request.sourceCode() == null || request.sourceCode().isBlank()) throw new IllegalArgumentException("Judge0 source_code must not be blank");
    }

    private Map<String, Object> buildPayload(Judge0Request request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language_id", request.languageId());
        payload.put("source_code", request.sourceCode());
        payload.put("stdin", request.stdin());
        if (request.expectedOutput() != null) payload.put("expected_output", request.expectedOutput());
        if (request.cpuTimeLimit() != null) payload.put("cpu_time_limit", request.cpuTimeLimit());
        if (request.memoryLimit() != null) payload.put("memory_limit", request.memoryLimit());
        if (log.isDebugEnabled()) {
            int codeLength = request.sourceCode() == null ? 0 : request.sourceCode().length();
            log.debug("Posting to Judge0: language_id={}, source_code_length={}, stdin_length={}", request.languageId(), codeLength, request.stdin() == null ? 0 : request.stdin().length());
        }
        return payload;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) headers.set("X-Auth-Token", apiKey);
        return headers;
    }

    private void logCompletedSubmission(Judge0Submission result) {
        if (result.status() != null) {
            log.debug("Submission completed with status: {} ({})", result.status().description(), result.status().id());
            if (result.isCompilationError()) log.debug("Compilation error: {}", result.compileOutput());
            else if (result.isRuntimeError()) log.debug("Runtime error: {}", result.stderr());
            else if (result.status().id() > 6) log.warn("Unexpected Judge0 status: {} ({})", result.status().description(), result.status().id());
        }
    }
}
