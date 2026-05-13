-- Horodatage du décès côté patient (verrou léger métier : écritures interdites après cette date).
ALTER TABLE patients ADD deceased_at TIMESTAMP;
