package com.finanzas.api;

public final class GoogleAuthorizationResult {
    private final String authorizationCode;
    private final String codeVerifier;
    private final String redirectUri;

    GoogleAuthorizationResult(String authorizationCode, String codeVerifier, String redirectUri) {
        this.authorizationCode = authorizationCode == null ? "" : authorizationCode;
        this.codeVerifier = codeVerifier == null ? "" : codeVerifier;
        this.redirectUri = redirectUri == null ? "" : redirectUri;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
