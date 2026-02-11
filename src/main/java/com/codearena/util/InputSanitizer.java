package com.codearena.util;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class InputSanitizer {

    public String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(input);
    }

    public String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "").trim();
    }

    public String sanitizeCode(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("\0", "")
                   .replace("\r\n", "\n")
                   .replace("\r", "\n");
    }

    public String sanitizeSlug(String input) {
        if (input == null) {
            return null;
        }
        return input.toLowerCase()
                   .replaceAll("[^a-z0-9\\-_]", "")
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", "");
    }
}
