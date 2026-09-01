package com.finanzas.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
public class ProductionSafetyValidator implements ApplicationRunner {
    private final Environment environment;
    private final String jwtSecret;
    private final String databasePassword;

    public ProductionSafetyValidator(Environment environment,
                                     @Value("${finanzas.jwt.secret}") String jwtSecret,
                                     @Value("${spring.datasource.password}") String databasePassword) {
        this.environment = environment;
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret;
        this.databasePassword = databasePassword == null ? "" : databasePassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
        if (!prod) {
            return;
        }
        requireStrong("FINANZAS_JWT_SECRET", jwtSecret, 48);
        requireStrong("POSTGRES_PASSWORD", databasePassword, 16);
    }

    private void requireStrong(String name, String value, int minLength) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.length() < minLength
                || lower.contains("change")
                || lower.contains("default")
                || lower.contains("dev")
                || lower.contains("password")
                || lower.contains("secret")) {
            throw new IllegalStateException(name + " es debil o predeterminado para produccion.");
        }
    }
}
