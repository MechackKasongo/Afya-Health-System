-- V13: Remove USER_ID NOT NULL constraint from refresh_tokens
-- The RefreshToken entity uses username (String) as its link to app_users,
-- not a JPA foreign key. The USER_ID column added in V11/V12 conflicts
-- with Hibernate inserts that never set it, causing ORA-01400.
-- Solution: make USER_ID nullable so legacy rows are kept and new inserts work.

DECLARE
    v_count NUMBER := 0;
BEGIN
    -- 1) Drop the FK constraint if it exists
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE table_name = 'REFRESH_TOKENS'
      AND UPPER(constraint_name) = 'FK_REFRESH_TOKENS_USER_ID';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens DROP CONSTRAINT fk_refresh_tokens_user_id';
    END IF;

    -- 2) Make USER_ID nullable (removes the NOT NULL constraint)
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'REFRESH_TOKENS'
      AND column_name = 'USER_ID'
      AND nullable = 'N';

    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens MODIFY (user_id NULL)';
    END IF;

END;
/
