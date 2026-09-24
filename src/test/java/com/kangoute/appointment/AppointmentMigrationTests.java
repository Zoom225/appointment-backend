package com.kangoute.appointment;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentMigrationTests {
    @Test
    void migrationPreservesExistingAppointmentsWithoutInventingHistoricalDates() throws Exception {
        String url = "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("create table appointments (id bigint primary key, status varchar(30), reason varchar(255))");
            statement.execute("insert into appointments values (42, 'SCHEDULED', 'Existing appointment')");
            statement.execute("create table users (id bigint primary key, email varchar(255))");
            statement.execute("insert into users values (7, 'existing@example.com')");
            Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(true).load().migrate();
            try (var row = statement.executeQuery("select id, demo_account_type from users")) {
                assertTrue(row.next());
                assertEquals(7, row.getLong("id"));
                assertEquals("NONE", row.getString("demo_account_type"));
                assertFalse(row.next());
            }
            try (var row = statement.executeQuery("select id, status, reason, created_at, updated_at from appointments")) {
                assertTrue(row.next());
                assertEquals(42, row.getLong("id"));
                assertEquals("SCHEDULED", row.getString("status"));
                assertEquals("Existing appointment", row.getString("reason"));
                assertNull(row.getTimestamp("created_at"));
                assertNull(row.getTimestamp("updated_at"));
                assertFalse(row.next());
            }
            try (var row = statement.executeQuery("select id from appointment_booking_lock")) {
                assertTrue(row.next());
                assertEquals(1, row.getLong(1));
                assertFalse(row.next());
            }
        }
    }
}
