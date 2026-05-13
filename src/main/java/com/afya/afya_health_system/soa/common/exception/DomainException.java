package com.afya.afya_health_system.soa.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for deterministic HTTP statuses (4xx mainly). Mapped by {@link com.afya.afya_health_system.soa.common.config.GlobalExceptionHandler}.
 * <p>Français : exception métier portant explicitement un {@link HttpStatus} pour le corps d'erreur API.</p>
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;

    protected DomainException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
