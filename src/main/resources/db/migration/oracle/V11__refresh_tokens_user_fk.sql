-- V11: Link refresh_tokens to app_users via user_id (idempotent, safe for existing data)
-- Fr: ajoute refresh_tokens.user_id, migre depuis username, puis pose FK + index.
-- Handles orphaned rows gracefully: assigns to admin user or deletes them.

DECLARE
    v_count NUMBER := 0;
    v_admin_id NUMBER(19,0) := NULL;
BEGIN
    -- 0) Get admin user ID for orphaned tokens (fallback to ID 1)
    BEGIN
        SELECT id INTO v_admin_id
        FROM app_users
        WHERE username = 'admin'
        AND ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            BEGIN
                SELECT id INTO v_admin_id FROM app_users WHERE id = 1 AND ROWNUM = 1;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    v_admin_id := NULL;
            END;
    END;

    -- 1) Add USER_ID column if missing (nullable first)
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'REFRESH_TOKENS'
      AND column_name = 'USER_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens ADD (user_id NUMBER(19,0))';
    END IF;

    -- 2) Backfill USER_ID from USERNAME when possible
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

    -- 3) Handle orphaned rows: assign to admin or delete
    IF v_admin_id IS NOT NULL THEN
        EXECUTE IMMEDIATE 'UPDATE refresh_tokens SET user_id = ' || v_admin_id || ' WHERE user_id IS NULL';
    ELSE
        -- No admin user found, delete orphaned tokens
        EXECUTE IMMEDIATE 'DELETE FROM refresh_tokens WHERE user_id IS NULL';
    END IF;

    -- 4) Enforce NOT NULL
    EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens MODIFY (user_id NOT NULL)';

    -- 5) Add FK on APP_USERS(ID) if missing
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE table_name = 'REFRESH_TOKENS'
      AND UPPER(constraint_name) = 'FK_REFRESH_TOKENS_USER_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES app_users(id)';
    END IF;

    -- 6) Add index for USER_ID lookups if missing
    SELECT COUNT(*)
    INTO v_count
    FROM user_indexes
    WHERE table_name = 'REFRESH_TOKENS'
      AND UPPER(index_name) = 'IDX_REFRESH_TOKENS_USER_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id)';
    END IF;
END;
/
