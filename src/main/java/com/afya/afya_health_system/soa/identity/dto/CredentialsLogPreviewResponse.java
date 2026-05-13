package com.afya.afya_health_system.soa.identity.dto;

/**
 * Preview of the plaintext credential export file (admin UI).
 */
public record CredentialsLogPreviewResponse(
        String content,
        /** True when no file exists or file has zero length. */
        boolean empty,
        /** Preview text may be truncated for huge files. */
        boolean truncated,
        long totalBytes,
        int lineCount
) {}
