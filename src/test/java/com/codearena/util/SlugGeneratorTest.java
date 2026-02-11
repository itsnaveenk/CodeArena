package com.codearena.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SlugGenerator}.
 * Tests slug generation from various title inputs.
 */
@DisplayName("SlugGenerator")
class SlugGeneratorTest {

    @Nested
    @DisplayName("generateSlug")
    class GenerateSlug {

        @Test
        @DisplayName("should convert simple title to lowercase slug")
        void shouldConvertSimpleTitleToLowercaseSlug() {
            String result = SlugGenerator.generateSlug("Two Sum");
            assertThat(result).isEqualTo("two-sum");
        }

        @Test
        @DisplayName("should replace spaces with hyphens")
        void shouldReplaceSpacesWithHyphens() {
            String result = SlugGenerator.generateSlug("Binary Search Tree");
            assertThat(result).isEqualTo("binary-search-tree");
        }

        @Test
        @DisplayName("should remove special characters")
        void shouldRemoveSpecialCharacters() {
            String result = SlugGenerator.generateSlug("Two Sum Problem!");
            assertThat(result).isEqualTo("two-sum-problem");
        }

        @Test
        @DisplayName("should handle multiple special characters")
        void shouldHandleMultipleSpecialCharacters() {
            String result = SlugGenerator.generateSlug("What's the Time? (Easy)");
            assertThat(result).isEqualTo("whats-the-time-easy");
        }

        @Test
        @DisplayName("should collapse multiple spaces into single hyphen")
        void shouldCollapseMultipleSpaces() {
            String result = SlugGenerator.generateSlug("Two   Sum    Problem");
            assertThat(result).isEqualTo("two-sum-problem");
        }

        @Test
        @DisplayName("should collapse multiple hyphens into single hyphen")
        void shouldCollapseMultipleHyphens() {
            String result = SlugGenerator.generateSlug("Two--Sum---Problem");
            assertThat(result).isEqualTo("two-sum-problem");
        }

        @Test
        @DisplayName("should remove leading and trailing hyphens")
        void shouldRemoveLeadingAndTrailingHyphens() {
            String result = SlugGenerator.generateSlug("  Two Sum  ");
            assertThat(result).isEqualTo("two-sum");
        }

        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            String result = SlugGenerator.generateSlug("Café Problem");
            assertThat(result).isEqualTo("cafe-problem");
        }

        @Test
        @DisplayName("should handle accented characters")
        void shouldHandleAccentedCharacters() {
            String result = SlugGenerator.generateSlug("Résumé Builder");
            assertThat(result).isEqualTo("resume-builder");
        }

        @Test
        @DisplayName("should return empty string for null input")
        void shouldReturnEmptyStringForNullInput() {
            String result = SlugGenerator.generateSlug(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for blank input")
        void shouldReturnEmptyStringForBlankInput() {
            String result = SlugGenerator.generateSlug("   ");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for empty input")
        void shouldReturnEmptyStringForEmptyInput() {
            String result = SlugGenerator.generateSlug("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle numbers in title")
        void shouldHandleNumbersInTitle() {
            String result = SlugGenerator.generateSlug("3Sum Problem");
            assertThat(result).isEqualTo("3sum-problem");
        }

        @Test
        @DisplayName("should handle title with only numbers")
        void shouldHandleTitleWithOnlyNumbers() {
            String result = SlugGenerator.generateSlug("123");
            assertThat(result).isEqualTo("123");
        }

        @Test
        @DisplayName("should handle mixed case")
        void shouldHandleMixedCase() {
            String result = SlugGenerator.generateSlug("TwoSum PROBLEM");
            assertThat(result).isEqualTo("twosum-problem");
        }

        @Test
        @DisplayName("should handle title with tabs and newlines")
        void shouldHandleTitleWithTabsAndNewlines() {
            String result = SlugGenerator.generateSlug("Two\tSum\nProblem");
            assertThat(result).isEqualTo("two-sum-problem");
        }
    }

    @Nested
    @DisplayName("generateSlugWithSuffix")
    class GenerateSlugWithSuffix {

        @Test
        @DisplayName("should return base slug when suffix is 0")
        void shouldReturnBaseSlugWhenSuffixIsZero() {
            String result = SlugGenerator.generateSlugWithSuffix("Two Sum", 0);
            assertThat(result).isEqualTo("two-sum");
        }

        @Test
        @DisplayName("should return base slug when suffix is negative")
        void shouldReturnBaseSlugWhenSuffixIsNegative() {
            String result = SlugGenerator.generateSlugWithSuffix("Two Sum", -1);
            assertThat(result).isEqualTo("two-sum");
        }

        @Test
        @DisplayName("should append suffix when positive")
        void shouldAppendSuffixWhenPositive() {
            String result = SlugGenerator.generateSlugWithSuffix("Two Sum", 1);
            assertThat(result).isEqualTo("two-sum-1");
        }

        @Test
        @DisplayName("should append larger suffix")
        void shouldAppendLargerSuffix() {
            String result = SlugGenerator.generateSlugWithSuffix("Two Sum", 42);
            assertThat(result).isEqualTo("two-sum-42");
        }
    }
}
