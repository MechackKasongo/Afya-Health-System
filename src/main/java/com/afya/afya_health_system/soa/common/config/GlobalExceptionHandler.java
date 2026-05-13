package com.afya.afya_health_system.soa.common.config;

import com.afya.afya_health_system.soa.common.exception.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central mapping of exceptions to HTTP responses.
 * Keeps API error payloads consistent for clients (status, error phrase, message, timestamp).
 * <p>Français : traduit toutes les exceptions gérées en réponses HTTP au format unique
 * (statut, libellé d'erreur, message, horodatage) pour les consommateurs de l'API.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * {@link com.afya.afya_health_system.soa.common.exception.NotFoundException},
     * {@link com.afya.afya_health_system.soa.common.exception.ConflictException},
     * {@link com.afya.afya_health_system.soa.common.exception.BadRequestException}, etc.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handleDomain(DomainException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "Conflit de donnees detecte");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        // Keeps auth/security payloads identical to domain errors / Même contrat JSON que les erreurs métier.
        int code = ex.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }
        return build(status, message);
    }

    /**
     * Shared JSON shape for all handled errors.
     * <p>Français : construit le corps JSON d'erreur standard.</p>
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
