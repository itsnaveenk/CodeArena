package com.codearena.integration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.codearena.config.Judge0Config;
import com.codearena.dto.Judge0Request;
import com.codearena.dto.Judge0Status;
import com.codearena.dto.Judge0Submission;
import com.codearena.exception.Judge0Exception;

@ExtendWith(MockitoExtension.class)
class Judge0ClientTest {

    @Mock
    private RestTemplate restTemplate;

    private Judge0Config config;
    private Judge0ClientImpl judge0Client;

    @BeforeEach
    void setUp() {
        config = new Judge0Config();
        config.setApiUrl("http://localhost:2358");
        config.setTimeout(30000);
        config.setPollInterval(100);
        judge0Client = new Judge0ClientImpl(restTemplate, config);
    }

    private Judge0Request createTestRequest() {
        return new Judge0Request(62, "public class Main { public static void main(String[] args) { System.out.println(\"Hello\"); } }", "");
    }

    @Nested
    @DisplayName("createSubmission tests")
    class CreateSubmissionTests {

        @Test
        @DisplayName("Should return submission when Judge0 responds successfully")
        void shouldReturnSubmissionOnSuccess() {
            Judge0Submission expected = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );

            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(expected));

            Judge0Submission result = judge0Client.createSubmission(createTestRequest());

            assertThat(result).isNotNull();
            assertThat(result.token()).isEqualTo("test-token");
        }

        @Test
        @DisplayName("Should throw Judge0Exception with 503 when Judge0 is unavailable")
        void shouldThrow503WhenJudge0Unavailable() {
            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> judge0Client.createSubmission(createTestRequest()))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("temporarily unavailable")
                .satisfies(e -> assertThat(((Judge0Exception) e).isTimeout()).isFalse());
        }

        @Test
        @DisplayName("Should throw Judge0Exception when Judge0 returns server error")
        void shouldThrowExceptionOnServerError() {
            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));

            assertThatThrownBy(() -> judge0Client.createSubmission(createTestRequest()))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("encountered an error");
        }

        @Test
        @DisplayName("Should throw Judge0Exception when response body is null")
        void shouldThrowExceptionOnNullResponse() {
            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(null));

            assertThatThrownBy(() -> judge0Client.createSubmission(createTestRequest()))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("empty response");
        }
    }

    @Nested
    @DisplayName("getSubmission tests")
    class GetSubmissionTests {

        @Test
        @DisplayName("Should return submission when polling succeeds")
        void shouldReturnSubmissionOnSuccess() {
            Judge0Submission expected = new Judge0Submission(
                "test-token", "Hello", null, null, null,
                new Judge0Status(3, "Accepted"), 0.5, 1024
            );

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(expected));

            Judge0Submission result = judge0Client.getSubmission("test-token");

            assertThat(result).isNotNull();
            assertThat(result.stdout()).isEqualTo("Hello");
            assertThat(result.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("Should throw Judge0Exception when Judge0 is unavailable during polling")
        void shouldThrowExceptionWhenUnavailableDuringPolling() {
            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> judge0Client.getSubmission("test-token"))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("temporarily unavailable");
        }
    }

    @Nested
    @DisplayName("createAndWaitForResult tests")
    class CreateAndWaitForResultTests {

        @Test
        @DisplayName("Should return result when execution completes within timeout")
        void shouldReturnResultOnSuccess() {
            Judge0Submission queued = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );
            Judge0Submission completed = new Judge0Submission(
                "test-token", "Hello", null, null, null,
                new Judge0Status(3, "Accepted"), 0.5, 1024
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(completed));

            Judge0Submission result = judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofSeconds(5)
            );

            assertThat(result).isNotNull();
            assertThat(result.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("Should throw Judge0Exception with timeout flag when execution times out")
        void shouldThrow504WhenJudge0TimesOut() {
            Judge0Submission queued = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(2, "Processing"), null, null
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            assertThatThrownBy(() -> judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofMillis(200)
            ))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("timed out")
                .satisfies(e -> assertThat(((Judge0Exception) e).isTimeout()).isTrue());
        }

        @Test
        @DisplayName("Should throw Judge0Exception when no token returned")
        void shouldThrowExceptionWhenNoToken() {
            Judge0Submission noToken = new Judge0Submission(
                null, null, null, null, null, null, null, null
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(noToken));

            assertThatThrownBy(() -> judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofSeconds(5)
            ))
                .isInstanceOf(Judge0Exception.class)
                .hasMessageContaining("did not return a submission token");
        }

        @Test
        @DisplayName("Should poll multiple times until completion")
        void shouldPollUntilCompletion() {
            Judge0Submission queued = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );
            Judge0Submission processing = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(2, "Processing"), null, null
            );
            Judge0Submission completed = new Judge0Submission(
                "test-token", "Output", null, null, null,
                new Judge0Status(3, "Accepted"), 0.1, 512
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            ))
                .thenReturn(ResponseEntity.ok(queued))
                .thenReturn(ResponseEntity.ok(processing))
                .thenReturn(ResponseEntity.ok(completed));

            Judge0Submission result = judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofSeconds(5)
            );

            assertThat(result.isAccepted()).isTrue();
            verify(restTemplate, atLeast(3)).exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            );
        }
    }

    @Nested
    @DisplayName("Error logging tests")
    class ErrorLoggingTests {

        @Test
        @DisplayName("Should handle compilation error status")
        void shouldHandleCompilationError() {
            Judge0Submission queued = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );
            Judge0Submission compilationError = new Judge0Submission(
                "test-token", null, null, "error: ';' expected", null,
                new Judge0Status(6, "Compilation Error"), null, null
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(compilationError));

            Judge0Submission result = judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofSeconds(5)
            );

            assertThat(result.isCompilationError()).isTrue();
            assertThat(result.compileOutput()).contains("';' expected");
        }

        @Test
        @DisplayName("Should handle runtime error status")
        void shouldHandleRuntimeError() {
            Judge0Submission queued = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );
            Judge0Submission runtimeError = new Judge0Submission(
                "test-token", null, "NullPointerException", null, null,
                new Judge0Status(11, "Runtime Error (NZEC)"), 0.1, 512
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(runtimeError));

            Judge0Submission result = judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofSeconds(5)
            );

            assertThat(result.isRuntimeError()).isTrue();
            assertThat(result.stderr()).contains("NullPointerException");
        }

        @Test
        @DisplayName("Should handle unexpected status codes")
        void shouldHandleUnexpectedStatus() {
            Judge0Submission queued = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );
            Judge0Submission unexpectedStatus = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(14, "Exec Format Error"), null, null
            );

            when(restTemplate.exchange(
                contains("submissions?"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(queued));

            when(restTemplate.exchange(
                contains("/submissions/test-token"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(unexpectedStatus));

            Judge0Submission result = judge0Client.createAndWaitForResult(
                createTestRequest(), Duration.ofSeconds(5)
            );

            assertThat(result.status().id()).isEqualTo(14);
        }
    }

    @Nested
    @DisplayName("API key configuration tests")
    class ApiKeyTests {

        @Test
        @DisplayName("Should include API key header when configured")
        void shouldIncludeApiKeyWhenConfigured() {
            config.setApiKey("test-api-key");
            judge0Client = new Judge0ClientImpl(restTemplate, config);

            Judge0Submission expected = new Judge0Submission(
                "test-token", null, null, null, null,
                new Judge0Status(1, "In Queue"), null, null
            );

            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                argThat(entity -> {
                    HttpEntity<?> httpEntity = (HttpEntity<?>) entity;
                    return "test-api-key".equals(httpEntity.getHeaders().getFirst("X-Auth-Token"));
                }),
                eq(Judge0Submission.class)
            )).thenReturn(ResponseEntity.ok(expected));

            judge0Client.createSubmission(createTestRequest());

            verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.POST),
                argThat(entity -> {
                    HttpEntity<?> httpEntity = (HttpEntity<?>) entity;
                    return "test-api-key".equals(httpEntity.getHeaders().getFirst("X-Auth-Token"));
                }),
                eq(Judge0Submission.class)
            );
        }
    }
}
