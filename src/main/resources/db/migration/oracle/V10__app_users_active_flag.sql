-- V10: Ensure app_users.active exists and is non-null (safe for non-empty tables)
-- Fr: garantit la colonne ACTIVE pour APP_USERS sans casser les données existantes.

DECLARE
    v_count NUMBER := 0;
BEGIN
    -- Add ACTIVE column if missing (nullable first to avoid ORA-01758 on populated tables)
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'APP_USERS'
      AND column_name = 'ACTIVE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE app_users ADD (active NUMBER(1,0))';
    END IF;

    -- Backfill existing rows
    EXECUTE IMMEDIATE 'UPDATE app_users SET active = 1 WHERE active IS NULL';

    -- Add default for new rows
    EXECUTE IMMEDIATE 'ALTER TABLE app_users MODIFY (active DEFAULT 1)';

    -- Enforce NOT NULL
    EXECUTE IMMEDIATE 'ALTER TABLE app_users MODIFY (active NOT NULL)';

    -- Add check constraint if missing
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE table_name = 'APP_USERS'
      AND constraint_name = 'CHK_APP_USERS_ACTIVE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE app_users ADD CONSTRAINT chk_app_users_active CHECK (active IN (0, 1))';
    END IF;
END;
/

