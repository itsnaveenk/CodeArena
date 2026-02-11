package com.codearena.util;

public interface OutputNormalizer {

    String normalize(String output);

    boolean areEqual(String actual, String expected);
}
