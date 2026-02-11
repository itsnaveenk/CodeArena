package com.codearena.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SupportedLanguage enum.
 */
class SupportedLanguageTest {

    @Test
    @DisplayName("Java should have Judge0 ID 62")
    void javaHasCorrectJudge0Id() {
        assertEquals(62, SupportedLanguage.JAVA.getJudge0Id());
        assertEquals("Java (OpenJDK 13.0.1)", SupportedLanguage.JAVA.getDisplayName());
    }

    @Test
    @DisplayName("Python should have Judge0 ID 71")
    void pythonHasCorrectJudge0Id() {
        assertEquals(71, SupportedLanguage.PYTHON.getJudge0Id());
        assertEquals("Python (3.8.1)", SupportedLanguage.PYTHON.getDisplayName());
    }

    @Test
    @DisplayName("C++ should have Judge0 ID 54")
    void cppHasCorrectJudge0Id() {
        assertEquals(54, SupportedLanguage.CPP.getJudge0Id());
        assertEquals("C++ (GCC 9.2.0)", SupportedLanguage.CPP.getDisplayName());
    }

    @Test
    @DisplayName("fromJudge0Id should return correct language for valid IDs")
    void fromJudge0IdReturnsCorrectLanguage() {
        assertEquals(SupportedLanguage.JAVA, SupportedLanguage.fromJudge0Id(62));
        assertEquals(SupportedLanguage.PYTHON, SupportedLanguage.fromJudge0Id(71));
        assertEquals(SupportedLanguage.CPP, SupportedLanguage.fromJudge0Id(54));
    }

    @Test
    @DisplayName("fromJudge0Id should throw exception for invalid ID")
    void fromJudge0IdThrowsExceptionForInvalidId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SupportedLanguage.fromJudge0Id(999)
        );
        assertEquals("Unsupported language ID: 999", exception.getMessage());
    }

    @Test
    @DisplayName("All enum values should be present")
    void allEnumValuesPresent() {
        SupportedLanguage[] values = SupportedLanguage.values();
        assertEquals(3, values.length);
        assertNotNull(SupportedLanguage.valueOf("JAVA"));
        assertNotNull(SupportedLanguage.valueOf("PYTHON"));
        assertNotNull(SupportedLanguage.valueOf("CPP"));
    }
}
