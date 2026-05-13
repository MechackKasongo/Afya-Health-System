-- V15: Drop the legacy FK_REFRESH_TOKENS_USERNAME constraint
-- Context: V1/V2 created a foreign key from refresh_tokens.username -> app_users.username
-- This constraint is preventing user deletion when orphaned refresh_tokens exist
-- V11-V14 attempted to migrate to user_id-based FK, but V14 never dropped this old constraint
-- Solution: Drop the old FK so user deletion works; refresh_tokens.username is now just a column

DECLARE
    v_count NUMBER := 0;
BEGIN
    -- Check if FK_REFRESH_TOKENS_USERNAME constraint exists
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE table_name = 'REFRESH_TOKENS'
      AND UPPER(constraint_name) = 'FK_REFRESH_TOKENS_USERNAME';

    -- Drop the constraint if it exists
    IF v_count > 0 THEN
        BEGIN
            EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens DROP CONSTRAINT fk_refresh_tokens_username';
        EXCEPTION
            WHEN OTHERS THEN
                -- If drop fails for any reason, log and continue
                NULL;
        END;
    END IF;

END;
/

