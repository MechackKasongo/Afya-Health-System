CREATE TABLE user_hospital_services (
    user_id NUMBER NOT NULL,
    hospital_service_id NUMBER NOT NULL,
    CONSTRAINT pk_user_hospital_services PRIMARY KEY (user_id, hospital_service_id),
    CONSTRAINT fk_uhs_user FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_uhs_hospital_service FOREIGN KEY (hospital_service_id) REFERENCES hospital_services(id) ON DELETE CASCADE
);

CREATE INDEX idx_uhs_user ON user_hospital_services(user_id);
CREATE INDEX idx_uhs_hospital_service ON user_hospital_services(hospital_service_id);
