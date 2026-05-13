-- V14: Make APP_USERS.ROLES column nullable (or drop it)
-- Context: APP_USERS.ROLES was a legacy VARCHAR2 column created by Hibernate
-- when ddl-auto=update was active (before V5 introduced the proper roles/user_roles tables).
-- The JPA entity AppUser now uses @ManyToMany via user_roles table, so this
-- column is unused. However it still exists with NOT NULL, causing ORA-01400
-- on every new user insert.
-- Fix: make the column nullable so Hibernate inserts are not blocked.

DECLARE
    v_count NUMBER := 0;
BEGIN
    -- Check if ROLES column exists in APP_USERS
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'APP_USERS'
      AND column_name = 'ROLES';

    IF v_count > 0 THEN
        -- Make it nullable (safe: existing data is preserved, new inserts work)
        EXECUTE IMMEDIATE 'ALTER TABLE app_users MODIFY (roles NULL)';
    END IF;
END;
/
