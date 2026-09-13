package com.finanzas.backend.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("finanzas_migration_test")
            .withUsername("finanzas")
            .withPassword("finanzas_test_password");

    @Test
    void migratesFromV3ToCurrentVersionOnRealPostgres() throws Exception {
        Flyway v3 = flyway().target("3").load();
        v3.clean();
        v3.migrate();

        assertEquals("3", currentVersion());

        Flyway latest = flyway().load();
        latest.migrate();

        assertEquals("8", currentVersion());
        assertTrue(tableExists("savings_config"));
        assertTrue(tableExists("savings_movements"));
        assertTrue(tableExists("account_tokens"));
        assertTrue(tableExists("user_settings"));
        assertTrue(tableExists("invoices"));
        assertTrue(tableExists("tax_configurations"));
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .cleanDisabled(false)
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
    }

    private String currentVersion() throws Exception {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select version from flyway_schema_history where success = true order by installed_rank desc limit 1")) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private boolean tableExists(String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             ResultSet result = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return result.next();
        }
    }
}
