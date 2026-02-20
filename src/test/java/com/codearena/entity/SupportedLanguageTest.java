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
    @DisplayName("JavaScript should have Judge0 ID 63")
    void javascriptHasCorrectJudge0Id() {
        assertEquals(63, SupportedLanguage.JAVASCRIPT.getJudge0Id());
        assertEquals("JavaScript (Node.js 12.14.0)", SupportedLanguage.JAVASCRIPT.getDisplayName());
    }

    @Test
    @DisplayName("C should have Judge0 ID 4")
    void cHasCorrectJudge0Id() {
        assertEquals(4, SupportedLanguage.C.getJudge0Id());
        assertEquals("C (GCC 8.3.0)", SupportedLanguage.C.getDisplayName());
    }

    @Test
    @DisplayName("TypeScript should have Judge0 ID 74")
    void typescriptHasCorrectJudge0Id() {
        assertEquals(74, SupportedLanguage.TYPESCRIPT.getJudge0Id());
        assertEquals("TypeScript (3.7.4)", SupportedLanguage.TYPESCRIPT.getDisplayName());
    }

    @Test
    @DisplayName("Go should have Judge0 ID 60")
    void goHasCorrectJudge0Id() {
        assertEquals(60, SupportedLanguage.GO.getJudge0Id());
        assertEquals("Go (1.13.5)", SupportedLanguage.GO.getDisplayName());
    }

    @Test
    @DisplayName("Rust should have Judge0 ID 73")
    void rustHasCorrectJudge0Id() {
        assertEquals(73, SupportedLanguage.RUST.getJudge0Id());
        assertEquals("Rust (1.40.0)", SupportedLanguage.RUST.getDisplayName());
    }

    @Test
    @DisplayName("fromJudge0Id should return correct language for valid IDs")
    void fromJudge0IdReturnsCorrectLanguage() {
        assertEquals(SupportedLanguage.JAVA, SupportedLanguage.fromJudge0Id(62));
        assertEquals(SupportedLanguage.PYTHON, SupportedLanguage.fromJudge0Id(71));
        assertEquals(SupportedLanguage.CPP, SupportedLanguage.fromJudge0Id(54));
        assertEquals(SupportedLanguage.JAVASCRIPT, SupportedLanguage.fromJudge0Id(63));
        assertEquals(SupportedLanguage.C, SupportedLanguage.fromJudge0Id(4));
        assertEquals(SupportedLanguage.TYPESCRIPT, SupportedLanguage.fromJudge0Id(74));
        assertEquals(SupportedLanguage.GO, SupportedLanguage.fromJudge0Id(60));
        assertEquals(SupportedLanguage.RUST, SupportedLanguage.fromJudge0Id(73));
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
        assertEquals(8, values.length);
        assertNotNull(SupportedLanguage.valueOf("JAVA"));
        assertNotNull(SupportedLanguage.valueOf("PYTHON"));
        assertNotNull(SupportedLanguage.valueOf("CPP"));
        assertNotNull(SupportedLanguage.valueOf("JAVASCRIPT"));
        assertNotNull(SupportedLanguage.valueOf("C"));
        assertNotNull(SupportedLanguage.valueOf("TYPESCRIPT"));
        assertNotNull(SupportedLanguage.valueOf("GO"));
        assertNotNull(SupportedLanguage.valueOf("RUST"));
    }
}
