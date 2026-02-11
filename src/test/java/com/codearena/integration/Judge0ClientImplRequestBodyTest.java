package com.codearena.integration;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.codearena.config.Judge0Config;
import com.codearena.dto.Judge0Request;

class Judge0ClientImplRequestBodyTest {

    @Test
    void createSubmission_shouldSendSnakeCaseFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        Judge0Config config = new Judge0Config();
        config.setApiUrl("http://localhost:2358");

        Judge0ClientImpl client = new Judge0ClientImpl(restTemplate, config);

        server.expect(requestTo("http://localhost:2358/submissions?base64_encoded=false&wait=false"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"language_id\":62")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"source_code\"")))
            .andRespond(withSuccess("{\"token\":\"abc\"}", MediaType.APPLICATION_JSON));

        client.createSubmission(new Judge0Request(62, "class Main{}", ""));

        server.verify();
    }
}
