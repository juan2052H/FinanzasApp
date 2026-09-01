package com.finanzas.api;

import java.io.IOException;

public class BackendApiException extends IOException {
    private final int statusCode;
    private final String errorCode;

    public BackendApiException(int statusCode, String message, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode == null ? "" : errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
