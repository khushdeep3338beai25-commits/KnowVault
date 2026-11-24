package com.knowvault.util;

public final class CsvUtil {
    private CsvUtil() { }
    public static String escape(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }
}
