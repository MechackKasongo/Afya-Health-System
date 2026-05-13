package com.afya.afya_health_system.soa.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
