-- Historical appointments remain intact; new bookings populate these nullable columns.
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS contact_first_name VARCHAR(80);
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS contact_last_name VARCHAR(80);
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS contact_email VARCHAR(254);
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS public_reference VARCHAR(40);
ALTER TABLE IF EXISTS appointments ADD COLUMN IF NOT EXISTS verification_token VARCHAR(64);
ALTER TABLE IF EXISTS appointments ADD CONSTRAINT ux_appointments_public_reference UNIQUE (public_reference);
ALTER TABLE IF EXISTS appointments ADD CONSTRAINT ux_appointments_verification_token UNIQUE (verification_token);
