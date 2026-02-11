package com.codearena.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the StringListConverter.
 */
class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    // Tests for convertToDatabaseColumn

    @Test
    @DisplayName("convertToDatabaseColumn should return null for null input")
    void convertToDatabaseColumnReturnsNullForNullInput() {
        String result = converter.convertToDatabaseColumn(null);
        assertNull(result);
    }

    @Test
    @DisplayName("convertToDatabaseColumn should return empty array for empty list")
    void convertToDatabaseColumnReturnsEmptyArrayForEmptyList() {
        String result = converter.convertToDatabaseColumn(new ArrayList<>());
        assertEquals("[]", result);
    }

    @Test
    @DisplayName("convertToDatabaseColumn should convert single element list")
    void convertToDatabaseColumnConvertsSingleElementList() {
        List<String> list = List.of("tag1");
        String result = converter.convertToDatabaseColumn(list);
        assertEquals("[\"tag1\"]", result);
    }

    @Test
    @DisplayName("convertToDatabaseColumn should convert multiple element list")
    void convertToDatabaseColumnConvertsMultipleElementList() {
        List<String> list = Arrays.asList("arrays", "dynamic-programming", "sorting");
        String result = converter.convertToDatabaseColumn(list);
        assertEquals("[\"arrays\",\"dynamic-programming\",\"sorting\"]", result);
    }

    @Test
    @DisplayName("convertToDatabaseColumn should handle special characters")
    void convertToDatabaseColumnHandlesSpecialCharacters() {
        List<String> list = List.of("tag with spaces", "tag-with-dashes", "tag_with_underscores");
        String result = converter.convertToDatabaseColumn(list);
        assertTrue(result.contains("tag with spaces"));
        assertTrue(result.contains("tag-with-dashes"));
        assertTrue(result.contains("tag_with_underscores"));
    }

    @Test
    @DisplayName("convertToDatabaseColumn should handle unicode characters")
    void convertToDatabaseColumnHandlesUnicodeCharacters() {
        List<String> list = List.of("算法", "アルゴリズム", "알고리즘");
        String result = converter.convertToDatabaseColumn(list);
        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
    }

    // Tests for convertToEntityAttribute

    @Test
    @DisplayName("convertToEntityAttribute should return empty list for null input")
    void convertToEntityAttributeReturnsEmptyListForNullInput() {
        List<String> result = converter.convertToEntityAttribute(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("convertToEntityAttribute should return empty list for empty string")
    void convertToEntityAttributeReturnsEmptyListForEmptyString() {
        List<String> result = converter.convertToEntityAttribute("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("convertToEntityAttribute should parse empty JSON array")
    void convertToEntityAttributeParsesEmptyJsonArray() {
        List<String> result = converter.convertToEntityAttribute("[]");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("convertToEntityAttribute should parse single element array")
    void convertToEntityAttributeParsesSingleElementArray() {
        List<String> result = converter.convertToEntityAttribute("[\"tag1\"]");
        assertEquals(1, result.size());
        assertEquals("tag1", result.get(0));
    }

    @Test
    @DisplayName("convertToEntityAttribute should parse multiple element array")
    void convertToEntityAttributeParsesMultipleElementArray() {
        List<String> result = converter.convertToEntityAttribute("[\"arrays\",\"dynamic-programming\",\"sorting\"]");
        assertEquals(3, result.size());
        assertEquals("arrays", result.get(0));
        assertEquals("dynamic-programming", result.get(1));
        assertEquals("sorting", result.get(2));
    }

    @Test
    @DisplayName("convertToEntityAttribute should handle special characters")
    void convertToEntityAttributeHandlesSpecialCharacters() {
        List<String> result = converter.convertToEntityAttribute("[\"tag with spaces\",\"tag-with-dashes\"]");
        assertEquals(2, result.size());
        assertEquals("tag with spaces", result.get(0));
        assertEquals("tag-with-dashes", result.get(1));
    }

    @Test
    @DisplayName("convertToEntityAttribute should tolerate invalid JSON by returning a singleton list")
    void convertToEntityAttributeToleratesInvalidJson() {
        List<String> result = converter.convertToEntityAttribute("not valid json");
        assertEquals(List.of("not valid json"), result);
    }

    @Test
    @DisplayName("convertToEntityAttribute should tolerate malformed JSON by returning a singleton list")
    void convertToEntityAttributeToleratesMalformedJsonArray() {
        List<String> result = converter.convertToEntityAttribute("[\"unclosed");
        assertEquals(List.of("[\"unclosed"), result);
    }

    @Test
    @DisplayName("convertToEntityAttribute should parse JSON-encoded string containing an array")
    void convertToEntityAttributeParsesJsonEncodedStringContainingArray() {
        // A JSON string whose value is a JSON array
        List<String> result = converter.convertToEntityAttribute("\"[\\\"ARRAY\\\"]\"");
        assertEquals(List.of("ARRAY"), result);
    }

    @Test
    @DisplayName("convertToEntityAttribute should parse double-encoded JSON string containing an array")
    void convertToEntityAttributeParsesDoubleEncodedJsonStringContainingArray() {
        // A JSON string whose value is another JSON string, whose value is a JSON array
        // i.e. "\"[\\\"ARRAY\\\"]\""
        List<String> result = converter.convertToEntityAttribute("\"\\\"[\\\\\\\"ARRAY\\\\\\\"]\\\"\"");
        assertEquals(List.of("ARRAY"), result);
    }

    @Test
    @DisplayName("convertToEntityAttribute should accept a single JSON string value as a singleton list")
    void convertToEntityAttributeAcceptsSingleJsonStringAsSingletonList() {
        List<String> result = converter.convertToEntityAttribute("\"dp\"");
        assertEquals(List.of("dp"), result);
    }

    // Round-trip tests

    @Test
    @DisplayName("Round-trip conversion should preserve data")
    void roundTripConversionPreservesData() {
        List<String> original = Arrays.asList("arrays", "dynamic-programming", "sorting", "binary-search");
        
        String json = converter.convertToDatabaseColumn(original);
        List<String> restored = converter.convertToEntityAttribute(json);
        
        assertEquals(original.size(), restored.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i), restored.get(i));
        }
    }

    @Test
    @DisplayName("Round-trip conversion should preserve empty list")
    void roundTripConversionPreservesEmptyList() {
        List<String> original = new ArrayList<>();
        
        String json = converter.convertToDatabaseColumn(original);
        List<String> restored = converter.convertToEntityAttribute(json);
        
        assertTrue(restored.isEmpty());
    }

    @Test
    @DisplayName("Round-trip conversion should preserve order")
    void roundTripConversionPreservesOrder() {
        List<String> original = Arrays.asList("z-tag", "a-tag", "m-tag");
        
        String json = converter.convertToDatabaseColumn(original);
        List<String> restored = converter.convertToEntityAttribute(json);
        
        assertEquals("z-tag", restored.get(0));
        assertEquals("a-tag", restored.get(1));
        assertEquals("m-tag", restored.get(2));
    }
}
