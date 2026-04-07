package de.hahnphilipp.watchwithfritzbox.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{([^\\}]+)\\}");

    public static String replacePlaceholders(String input, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholderContent = matcher.group(1);

            String replacement = processPlaceholder(placeholderContent, values);

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String processPlaceholder(String content, Map<String, String> values) {
        String[] parts = content.split(";");
        String variableName = parts[0];

        String value = values.getOrDefault(variableName, "");

        for (int i = 1; i < parts.length; i++) {
            value = applyAction(value, parts[i]);
        }

        return value;
    }

    private static String applyAction(String value, String action) {
        // LOWERCASE
        if ("LOWERCASE".equalsIgnoreCase(action)) {
            return value.toLowerCase();
        }

        // UPPERCASE
        if ("UPPERCASE".equalsIgnoreCase(action)) {
            return value.toUpperCase();
        }

        if ("TRIM".equalsIgnoreCase(action)) {
            return value.trim();
        }

        // URLENCODED
        if ("URLENCODED".equalsIgnoreCase(action)) {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return value;
            }
        }

        // REPLACE (z.B. ü=ue)
        if (action.contains("=")) {
            String[] parts = action.split("=", 2);
            String from = parts[0];
            String to = parts.length > 1 ? parts[1] : "";
            return value.replace(from, to);
        }

        return value;
    }
}
