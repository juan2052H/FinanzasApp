package com.finanzas.domain.security;

public class WorkspaceAccessException extends RuntimeException {
    public WorkspaceAccessException(String message) {
        super(message);
    }
}
