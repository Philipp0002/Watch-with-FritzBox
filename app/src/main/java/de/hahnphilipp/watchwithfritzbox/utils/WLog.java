package de.hahnphilipp.watchwithfritzbox.utils;

import java.time.LocalDateTime;

public class WLog {

    public static StringBuilder logBuilder;

    public static void init() {
        if(logBuilder == null)
            logBuilder = new StringBuilder();
    }

    private static void log(char level, String tag, String message) {
        init();
        logBuilder
                .append(LocalDateTime.now().toString())
                .append(" ")
                .append(level)
                .append(" ")
                .append(tag.toUpperCase())
                .append(" ")
                .append(message)
                .append("\n");
    }

    public static void i(String tag, String message) {
        log('I', tag, message);
    }

    public static void w(String tag, String message) {
        log('W', tag, message);
    }

    public static void e(String tag, String message) {
        log('E', tag, message);
    }

    public static String getLog() {
        init();
        return logBuilder.toString();
    }
}
