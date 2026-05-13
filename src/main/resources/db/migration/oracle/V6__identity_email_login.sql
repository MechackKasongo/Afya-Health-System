ALTER TABLE app_users ADD (
    email VARCHAR2(160 CHAR)
);

ALTER TABLE app_users ADD CONSTRAINT uq_app_users_email UNIQUE (email);
