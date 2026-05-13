-- V16: Blocklist for access JWT jti revoked at logout (purged rows when expires_at < systimestamp)
DECLARE
    v_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'REVOKED_ACCESS_JTI';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE revoked_access_jti (
                jti VARCHAR2(128 CHAR) NOT NULL,
                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                username VARCHAR2(80 CHAR) NOT NULL,
                CONSTRAINT pk_revoked_access_jti PRIMARY KEY (jti)
            )';
        EXECUTE IMMEDIATE '
            CREATE INDEX idx_revoked_access_jti_expires ON revoked_access_jti (expires_at)';
    END IF;
END;
/
