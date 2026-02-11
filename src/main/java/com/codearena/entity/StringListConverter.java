package com.codearena.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting list to JSON string", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }

        String jsonData = dbData.trim();

        if (!(jsonData.startsWith("[") || jsonData.startsWith("{") || jsonData.startsWith("\"") || "null".equals(jsonData))) {
            return new ArrayList<>(Collections.singletonList(jsonData));
        }

        try {
            for (int i = 0; i < 5; i++) {
                JsonNode node = objectMapper.readTree(jsonData);
                if (node == null) {
                    return new ArrayList<>();
                }

                if (node.isTextual()) {
                    String unwrapped = node.asText();
                    if (unwrapped == null || unwrapped.equals(jsonData)) {
                        break;
                    }
                    jsonData = unwrapped.trim();
                    continue;
                }
                break;
            }

            JsonNode finalNode = objectMapper.readTree(jsonData);
            if (finalNode == null || finalNode.isNull()) {
                return new ArrayList<>();
            }

            if (finalNode.isArray()) {
                return objectMapper.convertValue(finalNode, new TypeReference<List<String>>() {});
            }

            if (finalNode.isTextual()) {
                String single = finalNode.asText();
                if (single == null || single.isBlank()) {
                    return new ArrayList<>();
                }
                return new ArrayList<>(Collections.singletonList(single));
            }

            return objectMapper.readValue(jsonData, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>(Collections.singletonList(jsonData));
        }
    }
}
