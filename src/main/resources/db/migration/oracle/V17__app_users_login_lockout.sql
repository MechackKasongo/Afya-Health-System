-- Verrouillage après échecs de mot de passe : compteur + fenêtre jusqu'à déblocage automatique.
DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'APP_USERS'
      AND column_name = 'FAILED_LOGIN_ATTEMPTS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE app_users ADD (failed_login_attempts NUMBER(10) DEFAULT 0 NOT NULL)';
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_cols
    WHERE table_name = 'APP_USERS'
      AND column_name = 'LOCKED_UNTIL';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE app_users ADD (locked_until TIMESTAMP(6))';
    END IF;
END;
/
