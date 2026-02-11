package com.codearena.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Tag;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.StringLength;

/**
 * Property-based tests for OutputNormalizer.
 */
@Tag("Feature: codearena-platform")
class OutputNormalizerProperties {

    private final OutputNormalizer normalizer = new DefaultOutputNormalizer();

    /**
     * Property 22: Output Normalization Equivalence
     * For any two strings that differ only in leading/trailing whitespace or line ending style
     * (CRLF vs LF), the Output_Normalizer SHALL consider them equal.
     *
     * **Validates: Requirements 10.9**
     */
    @Property(tries = 100)
    @Tag("Property 22: Output Normalization Equivalence")
    void outputNormalizationEquivalence(
        @ForAll @StringLength(min = 0, max = 500) String base,
        @ForAll("whitespaceVariations") String leadingWs,
        @ForAll("whitespaceVariations") String trailingWs
    ) {
        // Create a version with added whitespace
        String withWhitespace = leadingWs + base + trailingWs;

        // After normalization, they should be equal
        assertThat(normalizer.areEqual(withWhitespace, base)).isTrue();
    }

    @Property(tries = 100)
    @Tag("Property 22a: CRLF to LF Normalization")
    void crlfToLfNormalization(
        @ForAll @StringLength(min = 1, max = 100) String line1,
        @ForAll @StringLength(min = 1, max = 100) String line2
    ) {
        // Create strings with different line endings
        String withLf = line1 + "\n" + line2;
        String withCrlf = line1 + "\r\n" + line2;
        String withCr = line1 + "\r" + line2;

        // All should normalize to the same result
        String normalizedLf = normalizer.normalize(withLf);
        String normalizedCrlf = normalizer.normalize(withCrlf);
        String normalizedCr = normalizer.normalize(withCr);

        assertThat(normalizedLf).isEqualTo(normalizedCrlf);
        assertThat(normalizedLf).isEqualTo(normalizedCr);
    }

    @Property(tries = 100)
    @Tag("Property 22b: Leading/Trailing Whitespace Trimming")
    void leadingTrailingWhitespaceTrimming(
        @ForAll @StringLength(min = 1, max = 200) String content
    ) {
        // Add various whitespace combinations
        String withLeadingSpaces = "   " + content;
        String withTrailingSpaces = content + "   ";
        String withLeadingTabs = "\t\t" + content;
        String withTrailingNewlines = content + "\n\n";
        String withMixed = "  \t\n" + content + "\n\t  ";

        String expected = content.trim();

        assertThat(normalizer.normalize(withLeadingSpaces)).isEqualTo(expected);
        assertThat(normalizer.normalize(withTrailingSpaces)).isEqualTo(expected);
        assertThat(normalizer.normalize(withLeadingTabs)).isEqualTo(expected);
        assertThat(normalizer.normalize(withTrailingNewlines)).isEqualTo(expected);
        assertThat(normalizer.normalize(withMixed)).isEqualTo(expected);
    }

    @Property(tries = 100)
    @Tag("Property 22c: Null Safety")
    void nullSafety() {
        assertThat(normalizer.normalize(null)).isEqualTo("");
        assertThat(normalizer.areEqual(null, "")).isTrue();
        assertThat(normalizer.areEqual("", null)).isTrue();
        assertThat(normalizer.areEqual(null, null)).isTrue();
    }

    @Property(tries = 100)
    @Tag("Property 22d: Idempotence")
    void normalizationIsIdempotent(
        @ForAll @StringLength(min = 0, max = 300) String input
    ) {
        // Normalizing twice should give the same result as normalizing once
        String normalizedOnce = normalizer.normalize(input);
        String normalizedTwice = normalizer.normalize(normalizedOnce);

        assertThat(normalizedOnce).isEqualTo(normalizedTwice);
    }

    @Property(tries = 100)
    @Tag("Property 22e: Internal Whitespace Preserved")
    void internalWhitespacePreserved(
        @ForAll @StringLength(min = 1, max = 100) String word1,
        @ForAll @StringLength(min = 1, max = 100) String word2,
        @ForAll("internalWhitespace") String separator
    ) {
        String input = word1 + separator + word2;
        String normalized = normalizer.normalize(input);

        // Internal whitespace should be preserved (except line ending normalization)
        String expectedSeparator = separator.replace("\r\n", "\n").replace("\r", "\n");
        assertThat(normalized).contains(expectedSeparator);
    }

    @Provide
    Arbitrary<String> whitespaceVariations() {
        return Arbitraries.oneOf(
            Arbitraries.just(""),
            Arbitraries.just(" "),
            Arbitraries.just("  "),
            Arbitraries.just("\t"),
            Arbitraries.just("\n"),
            Arbitraries.just("\r\n"),
            Arbitraries.just("  \t"),
            Arbitraries.just("\n\n"),
            Arbitraries.just("  \n\t  ")
        );
    }

    @Provide
    Arbitrary<String> internalWhitespace() {
        return Arbitraries.oneOf(
            Arbitraries.just(" "),
            Arbitraries.just("  "),
            Arbitraries.just("\t"),
            Arbitraries.just("\n"),
            Arbitraries.just("\r\n"),
            Arbitraries.just("   ")
        );
    }
}
