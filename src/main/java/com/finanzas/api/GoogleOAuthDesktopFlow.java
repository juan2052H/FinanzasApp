package com.finanzas.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class GoogleOAuthDesktopFlow {
    private static final SecureRandom RANDOM = new SecureRandom();

    public GoogleAuthorizationResult authorize(String clientId) throws IOException, InterruptedException {
        String safeClientId = clientId == null ? "" : clientId.trim();
        if (safeClientId.isEmpty()) {
            throw new IOException("GOOGLE_OAUTH_CLIENT_ID no esta configurado.");
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("No fue posible abrir el navegador del sistema.");
        }

        String codeVerifier = randomToken(32);
        String state = randomToken(24);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String redirectUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> codeRef = new AtomicReference<String>();
        AtomicReference<Exception> errorRef = new AtomicReference<Exception>();

        server.createContext("/callback", exchange -> {
            try {
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                if (!state.equals(query.get("state"))) {
                    throw new IOException("Respuesta OAuth invalida.");
                }
                String error = query.get("error");
                if (error != null && !error.trim().isEmpty()) {
                    throw new IOException("Google rechazo el inicio de sesion: " + error);
                }
                String code = query.get("code");
                if (code == null || code.trim().isEmpty()) {
                    throw new IOException("Google no devolvio codigo de autorizacion.");
                }
                codeRef.set(code);
                respond(exchange, "FinanzasApp", "Autorizacion recibida. Puedes volver a FinanzasApp.");
            } catch (Exception ex) {
                errorRef.set(ex);
                respond(exchange, "FinanzasApp", "No fue posible completar el inicio de sesion.");
            } finally {
                latch.countDown();
            }
        });
        server.start();
        try {
            Desktop.getDesktop().browse(URI.create(authorizationUrl(safeClientId, redirectUri, codeVerifier, state)));
            if (!latch.await(Duration.ofMinutes(3).toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IOException("Tiempo agotado esperando la autorizacion de Google.");
            }
            if (errorRef.get() != null) {
                Exception error = errorRef.get();
                if (error instanceof IOException) {
                    throw (IOException) error;
                }
                throw new IOException(error.getMessage(), error);
            }
            return new GoogleAuthorizationResult(codeRef.get(), codeVerifier, redirectUri);
        } finally {
            server.stop(0);
        }
    }

    private String authorizationUrl(String clientId, String redirectUri, String codeVerifier, String state) throws IOException {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("response_type", "code");
        params.put("scope", "openid email profile");
        params.put("code_challenge", codeChallenge(codeVerifier));
        params.put("code_challenge_method", "S256");
        params.put("state", state);
        params.put("access_type", "offline");
        params.put("prompt", "select_account");
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) {
                query.append('&');
            }
            query.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return "https://accounts.google.com/o/oauth2/v2/auth?" + query;
    }

    private String codeChallenge(String verifier) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IOException("No fue posible crear PKCE challenge.", ex);
        }
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private Map<String, String> parseQuery(String rawQuery) throws IOException {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return result;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length > 1 ? urlDecode(parts[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    private String urlEncode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private String urlDecode(String value) throws IOException {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    }

    private void respond(HttpExchange exchange, String title, String message) throws IOException {
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>"
                + title
                + "</title></head><body><h1>"
                + title
                + "</h1><p>"
                + message
                + "</p></body></html>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
