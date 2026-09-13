package com.finanzas.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String INSECURE_DEFAULT_SECRET = "dev-only-change-this-secret-before-real-use";
    private final byte[] secret;
    private final long accessTokenSeconds;

    public JwtService(@Value("${finanzas.jwt.secret}") String secret,
                      @Value("${finanzas.jwt.access-token-minutes}") long accessTokenMinutes,
                      Environment environment) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("FINANZAS_JWT_SECRET debe tener al menos 32 caracteres.");
        }
        if (INSECURE_DEFAULT_SECRET.equals(secret) && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            throw new IllegalStateException(
                    "FINANZAS_JWT_SECRET sigue usando el valor de desarrollo por defecto con el perfil de produccion activo. "
                            + "Define un secreto propio antes de desplegar.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSeconds = Math.max(1, accessTokenMinutes) * 60L;
    }

    public String createAccessToken(UUID userId, String email) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userId.toString());
        payload.put("email", email);
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plusSeconds(accessTokenSeconds).getEpochSecond());
        String unsigned = base64Json(header) + "." + base64Json(payload);
        return unsigned + "." + sign(unsigned);
    }

    public AuthenticatedUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) {
                return null;
            }
            Map<?, ?> payload = JSON.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            Number exp = (Number) payload.get("exp");
            if (exp == null || Instant.now().getEpochSecond() >= exp.longValue()) {
                return null;
            }
            return new AuthenticatedUser(UUID.fromString((String) payload.get("sub")), (String) payload.get("email"));
        } catch (Exception ex) {
            return null;
        }
    }

    private String base64Json(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible crear el token.", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible firmar el token.", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
