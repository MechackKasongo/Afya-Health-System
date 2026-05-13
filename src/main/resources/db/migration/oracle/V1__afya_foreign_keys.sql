-- =============================================================================
-- AFYA — Foreign keys (Oracle)
-- =============================================================================
-- Prerequisites:
--   • Tables must already exist (typically created by Hibernate with ddl-auto=update).
--   • Run once with spring.flyway.enabled=true (or FLYWAY_ENABLED=true).
--   • If any INSERT fails with ORA-02291, clean orphan rows first (child IDs that
--     reference missing parents).
--
-- This script is idempotent: constraints are added only if missing (safe re-run).
-- =============================================================================

-- admissions -> patients
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_ADMISSIONS_PATIENT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE admissions ADD CONSTRAINT fk_admissions_patient FOREIGN KEY (patient_id) REFERENCES patients (id)]';
  END IF;
END;
/

-- consultations -> patients, admissions
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_CONSULTATIONS_PATIENT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE consultations ADD CONSTRAINT fk_consultations_patient FOREIGN KEY (patient_id) REFERENCES patients (id)]';
  END IF;
END;
/

DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_CONSULTATIONS_ADMISSION';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE consultations ADD CONSTRAINT fk_consultations_admission FOREIGN KEY (admission_id) REFERENCES admissions (id)]';
  END IF;
END;
/

-- consultation_events -> consultations, patients
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_CE_CONSULTATION';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE consultation_events ADD CONSTRAINT fk_ce_consultation FOREIGN KEY (consultation_id) REFERENCES consultations (id)]';
  END IF;
END;
/

DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_CE_PATIENT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE consultation_events ADD CONSTRAINT fk_ce_patient FOREIGN KEY (patient_id) REFERENCES patients (id)]';
  END IF;
END;
/

-- medical_records / medical_record_entries -> patients
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_MR_PATIENT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE medical_records ADD CONSTRAINT fk_mr_patient FOREIGN KEY (patient_id) REFERENCES patients (id)]';
  END IF;
END;
/

DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_MRE_PATIENT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE medical_record_entries ADD CONSTRAINT fk_mre_patient FOREIGN KEY (patient_id) REFERENCES patients (id)]';
  END IF;
END;
/

-- urgences -> patients
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_URGENCES_PATIENT';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE urgences ADD CONSTRAINT fk_urgences_patient FOREIGN KEY (patient_id) REFERENCES patients (id)]';
  END IF;
END;
/

-- urgence_timeline_events -> urgences
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_UTE_URGENCE';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE urgence_timeline_events ADD CONSTRAINT fk_ute_urgence FOREIGN KEY (urgence_id) REFERENCES urgences (id)]';
  END IF;
END;
/

-- admission_movements -> admissions
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_AM_ADMISSION';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE admission_movements ADD CONSTRAINT fk_am_admission FOREIGN KEY (admission_id) REFERENCES admissions (id)]';
  END IF;
END;
/

-- refresh_tokens.username -> app_users.username (unique key)
DECLARE
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_cnt FROM user_constraints WHERE constraint_name = 'FK_REFRESH_TOKENS_USERNAME';
  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE q'[ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_username FOREIGN KEY (username) REFERENCES app_users (username)]';
  END IF;
END;
/
