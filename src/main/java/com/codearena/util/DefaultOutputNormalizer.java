package com.codearena.util;

import org.springframework.stereotype.Component;

@Component
public class DefaultOutputNormalizer implements OutputNormalizer {

    @Override
    public String normalize(String output) {
        if (output == null) {
            return "";
        }
        return output
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim();
    }

    @Override
    public boolean areEqual(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }
}
