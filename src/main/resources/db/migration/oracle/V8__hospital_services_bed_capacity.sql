ALTER TABLE hospital_services ADD (
    bed_capacity NUMBER(10) DEFAULT 1 NOT NULL
);

UPDATE hospital_services
SET bed_capacity = CASE UPPER(name)
    WHEN UPPER('Urgences') THEN 20
    WHEN UPPER('Médecine interne') THEN 30
    WHEN UPPER('Chirurgie') THEN 25
    WHEN UPPER('Pédiatrie') THEN 20
    WHEN UPPER('Maternité') THEN 15
    ELSE 10
END
WHERE bed_capacity IS NULL OR bed_capacity <= 1;
