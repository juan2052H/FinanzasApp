package com.finanzas.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String PBKDF2_PREFIX = "pbkdf2:";
    private static final String SHA256_PREFIX = "sha256:";
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AuthService() {
    }

    static boolean login(Map<String, DataManager.UserProfile> profiles, String email, String password) {
        DataManager.UserProfile profile = profiles.get(normalizeEmail(email));
        return profile != null && matchesPassword(profile, password);
    }

    static boolean register(Map<String, DataManager.UserProfile> profiles,
                            String nombreCompleto, String email, String password, String moneda, String tipoCuenta) {
        String normalizedEmail = normalizeEmail(email);
        if (!isValidEmail(normalizedEmail) || password == null || password.length() < 6 || profiles.containsKey(normalizedEmail)) {
            return false;
        }

        DataManager.UserProfile profile = new DataManager.UserProfile(nombreCompleto, normalizedEmail, password, moneda, tipoCuenta);
        profile.password = hashPassword(password);
        profiles.put(normalizedEmail, profile);
        return true;
    }

    static boolean changePassword(DataManager.UserProfile profile, String currentPassword, String newPassword) {
        if (profile == null || !matchesPassword(profile, currentPassword)) {
            return false;
        }
        profile.password = hashPassword(newPassword);
        return true;
    }

    static void migratePasswords(Map<String, DataManager.UserProfile> profiles) {
        for (DataManager.UserProfile profile : profiles.values()) {
            if (profile != null && profile.password != null && !isPbkdf2(profile.password) && !isLegacySha256(profile.password)) {
                profile.password = hashPassword(profile.password);
            }
        }
    }

    static List<String> allUsers(Map<String, DataManager.UserProfile> profiles) {
        return profiles.values().stream()
                .map(profile -> profile.usuario.getNombreCompleto())
                .collect(Collectors.toList());
    }

    static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static boolean matchesPassword(DataManager.UserProfile profile, String rawPassword) {
        if (profile.password == null) {
            return false;
        }
        if (isPbkdf2(profile.password)) {
            return verifyPbkdf2(profile.password, rawPassword);
        }
        if (isLegacySha256(profile.password)) {
            boolean matches = profile.password.equals(legacySha256(rawPassword));
            if (matches) {
                profile.password = hashPassword(rawPassword);
            }
            return matches;
        }
        boolean matches = profile.password.equals(rawPassword);
        if (matches) {
            profile.password = hashPassword(rawPassword);
        }
        return matches;
    }

    private static boolean isPbkdf2(String value) {
        return value != null && value.startsWith(PBKDF2_PREFIX);
    }

    private static boolean isLegacySha256(String value) {
        return value != null && value.startsWith(SHA256_PREFIX);
    }

    private static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
        return PBKDF2_PREFIX
                + PBKDF2_ITERATIONS
                + ":"
                + Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPbkdf2(String storedPassword, String rawPassword) {
        try {
            String[] parts = storedPassword.split(":");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword, salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            KeySpec spec = new PBEKeySpec((password == null ? "" : password).toCharArray(), salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible proteger la contrasena.", ex);
        }
    }

    private static String legacySha256(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(SHA256_PREFIX);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible proteger la contrasena.", ex);
        }
    }
}
