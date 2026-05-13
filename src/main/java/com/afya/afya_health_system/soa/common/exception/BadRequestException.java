package com.afya.afya_health_system.soa.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Semantic 400 payloads (validation / règle métier côté requête).
 * <p>Français : requête mal formée ou règle de saisie non respectée → HTTP 400.</p>
 */
public class BadRequestException extends DomainException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
