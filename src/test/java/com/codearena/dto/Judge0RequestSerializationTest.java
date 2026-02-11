package com.codearena.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class Judge0RequestSerializationTest {

    @Test
    void shouldSerializeSnakeCaseForJudge0() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

        Judge0Request request = new Judge0Request(62, "print('ok')", "1\n");
        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"language_id\":62");
        assertThat(json).contains("\"source_code\"");
        assertThat(json).contains("\"stdin\"");
    }
}
