package com.codearena.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.codearena.config.Judge0Config;
import com.codearena.dto.Judge0Request;

@ExtendWith(MockitoExtension.class)
class Judge0ClientImplValidationTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void createSubmission_shouldRejectNullRequest() {
        Judge0ClientImpl client = new Judge0ClientImpl(restTemplate, config());

        assertThatThrownBy(() -> client.createSubmission(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void createSubmission_shouldRejectBlankSourceCode() {
        Judge0ClientImpl client = new Judge0ClientImpl(restTemplate, config());

        Judge0Request request = new Judge0Request(62, "   ", "");

        assertThatThrownBy(() -> client.createSubmission(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source_code");

        verifyNoInteractions(restTemplate);
    }

    @Test
    void createSubmission_shouldRejectNonPositiveLanguageId() {
        Judge0ClientImpl client = new Judge0ClientImpl(restTemplate, config());

        Judge0Request request = new Judge0Request(0, "print('ok')", "");

        assertThatThrownBy(() -> client.createSubmission(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("language_id");

        verifyNoInteractions(restTemplate);
    }

    private static Judge0Config config() {
        Judge0Config config = new Judge0Config();
        config.setApiUrl("http://localhost:2358");
        config.setPollInterval(10);
        return config;
    }
}
