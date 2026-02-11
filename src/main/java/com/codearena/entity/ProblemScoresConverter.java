package com.codearena.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProblemScoresConverter implements AttributeConverter<List<ProblemScore>, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ProblemScore> scores) {
        try {
            return scores == null ? "[]" : mapper.writeValueAsString(scores);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<ProblemScore> convertToEntityAttribute(String json) {
        try {
            return json == null ? new ArrayList<>() :
                mapper.readValue(json, new TypeReference<List<ProblemScore>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
