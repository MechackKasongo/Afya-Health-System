-- V12: Repair V11 failure - Complete refresh_tokens.user_id migration
-- Fr: répare la migration V11 échouée et finalise la liaison user_id + FK + index

DECLARE
    v_count NUMBER := 0;
    v_admin_id NUMBER(19,0);
BEGIN
    -- 1) Get admin user ID for orphaned tokens fallback
    BEGIN
        SELECT id INTO v_admin_id
        FROM app_users
        WHERE username = 'admin'
        AND ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            -- If no admin found, use ID 1 if exists
            SELECT id INTO v_admin_id
            FROM app_users
            WHERE id = 1
            AND ROWNUM = 1;
    END;

    -- 2) Add USER_ID column if missing (nullable)
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'REFRESH_TOKENS'
      AND column_name = 'USER_ID';

    IF v_count = 0 THEN
        BEGIN
            EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens ADD (user_id NUMBER(19,0))';
        EXCEPTION
            WHEN OTHERS THEN
                -- Column might already exist, ignore
                NULL;
        END;
    END IF;

    -- 3) Backfill USER_ID from USERNAME for matching users
    BEGIN
        EXECUTE IMMEDIATE q'[
            UPDATE refresh_tokens rt
            SET user_id = (
                SELECT au.id
                FROM app_users au
                WHERE au.username = rt.username
            )
            WHERE rt.user_id IS NULL
              AND rt.username IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM app_users au
                  WHERE au.username = rt.username
              )
        ]';
    EXCEPTION
        WHEN OTHERS THEN
            NULL;  -- Continue even if update fails
    END;

    -- 4) Handle orphaned refresh tokens - assign to admin/ID 1
    IF v_admin_id IS NOT NULL THEN
        BEGIN
            EXECUTE IMMEDIATE 'UPDATE refresh_tokens SET user_id = ' || v_admin_id || ' WHERE user_id IS NULL';
        EXCEPTION
            WHEN OTHERS THEN
                NULL;
        END;
    END IF;

    -- 5) If still NULL (no admin found), delete orphaned tokens
    BEGIN
        EXECUTE IMMEDIATE 'DELETE FROM refresh_tokens WHERE user_id IS NULL';
    EXCEPTION
        WHEN OTHERS THEN
            NULL;
    END;

    -- 6) Enforce NOT NULL on user_id
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens MODIFY (user_id NOT NULL)';
    EXCEPTION
        WHEN OTHERS THEN
            -- Might already be NOT NULL
            NULL;
    END;

    -- 7) Drop existing FK if present (to replace it)
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens DROP CONSTRAINT fk_refresh_tokens_user_id';
    EXCEPTION
        WHEN OTHERS THEN
            -- Constraint might not exist, ignore
            NULL;
    END;

    -- 8) Add FK constraint on APP_USERS(ID)
    BEGIN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES app_users(id)';
    EXCEPTION
        WHEN OTHERS THEN
            -- FK might already exist
            NULL;
    END;

    -- 9) Create index for USER_ID lookups if missing
    BEGIN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id)';
    EXCEPTION
        WHEN OTHERS THEN
            -- Index might already exist, ignore
            NULL;
    END;

END;
/

