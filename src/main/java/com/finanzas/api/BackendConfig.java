package com.finanzas.api;

public final class BackendConfig {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private BackendConfig() {
    }

    public static boolean isEnabled() {
        String value = firstNonBlank(
                System.getProperty("finanzas.api.enabled"),
                System.getenv("FINANZAS_API_ENABLED"));
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    public static String baseUrl() {
        return trimTrailingSlash(firstNonBlank(
                System.getProperty("finanzas.api.baseUrl"),
                System.getenv("FINANZAS_API_BASE_URL"),
                DEFAULT_BASE_URL));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimTrailingSlash(String value) {
        String result = value == null ? DEFAULT_BASE_URL : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
