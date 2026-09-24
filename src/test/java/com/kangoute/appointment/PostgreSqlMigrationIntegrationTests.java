package com.kangoute.appointment;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTests {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void migrationsPreserveExistingDataAndMarkRealUsersAsNonDemo() throws Exception {
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("create table appointments (id bigint primary key, status varchar(30), reason varchar(255))");
            statement.execute("insert into appointments values (42, 'SCHEDULED', 'Historical appointment')");
            statement.execute("create table users (id bigint primary key, email varchar(255))");
            statement.execute("insert into users values (7, 'real.admin@example.com')");

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();

            try (var result = statement.executeQuery("select version from flyway_schema_history where success order by installed_rank desc limit 1")) {
                assertTrue(result.next());
                assertEquals("5", result.getString(1));
            }
            try (var result = statement.executeQuery("select id, email, demo_account_type from users")) {
                assertTrue(result.next());
                assertEquals(7L, result.getLong("id"));
                assertEquals("real.admin@example.com", result.getString("email"));
                assertEquals("NONE", result.getString("demo_account_type"));
                assertFalse(result.next());
            }
            try (var result = statement.executeQuery("select id from appointment_booking_lock")) {
                assertTrue(result.next());
                assertEquals(1L, result.getLong(1));
                assertFalse(result.next());
            }
            try (var result = statement.executeQuery("select * from appointments")) {
                assertTrue(result.next());
                assertEquals(42L, result.getLong("id"));
                assertEquals("SCHEDULED", result.getString("status"));
                assertEquals("Historical appointment", result.getString("reason"));
                assertNull(result.getTimestamp("created_at"));
                assertNull(result.getTimestamp("updated_at"));
                assertNull(result.getString("contact_first_name"));
                assertNull(result.getString("contact_last_name"));
                assertNull(result.getString("contact_email"));
                assertNull(result.getString("public_reference"));
                assertNull(result.getString("verification_token"));
                assertFalse(result.next());
            }
            statement.execute("update appointments set public_reference='RDV-test', verification_token='test-token' where id=42");
            var duplicateReference = org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class,
                    () -> statement.execute("insert into appointments (id, public_reference) values (43, 'RDV-test')"));
            assertEquals("23505", duplicateReference.getSQLState());
            var duplicateToken = org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class,
                    () -> statement.execute("insert into appointments (id, verification_token) values (44, 'test-token')"));
            assertEquals("23505", duplicateToken.getSQLState());
        }
    }
}
