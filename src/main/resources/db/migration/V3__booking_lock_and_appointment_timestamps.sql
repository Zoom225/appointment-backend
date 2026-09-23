-- A stable row serializes calendar writes, including reservations of empty slots.
CREATE TABLE appointment_booking_lock (id BIGINT PRIMARY KEY);
INSERT INTO appointment_booking_lock (id) VALUES (1);

-- Existing dates are unknown: retain NULL rather than invent historical timestamps.
-- On a fresh database Hibernate creates appointments after Flyway.
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
