ALTER TABLE IF EXISTS appointments
DROP CONSTRAINT IF EXISTS appointments_status_check;

ALTER TABLE IF EXISTS appointments
ADD CONSTRAINT appointments_status_check
CHECK (
    status IN (
        'PENDING',
        'SCHEDULED',
        'CONFIRMED',
        'CANCELLED',
        'COMPLETED'
    )
);
