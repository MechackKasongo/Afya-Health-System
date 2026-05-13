package com.afya.afya_health_system.soa.identity.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Year;
import java.util.Arrays;
import java.util.Random;

/**
 * Mot de passe généré au format {@link StandardGeneratedPassword}, avec graine dérivée de
 * {@code SHA-256(username + nom + année + ordinal)} pour un résultat déterministe par identité.
 */
public final class SecurePasswordGenerator {

    private SecurePasswordGenerator() {}

    /**
     * Seed logique : username + nom complet + année civile + numéro (ordinal / prochain id).
     */
    public static String generateFromIdentity(
            String fullName,
            String username,
            int year,
            long ordinal,
            int length
    ) {
        String seedMaterial = normalize(username)
                + "|"
                + normalize(fullName)
                + "|"
                + year
                + "|"
                + ordinal;
        byte[] hash = sha256(seedMaterial.getBytes(StandardCharsets.UTF_8));
        long seedLong = ByteBuffer.wrap(Arrays.copyOf(hash, 8)).getLong()
                ^ (ordinal * 0x9E3779B97F4A7C15L)
                ^ ((long) year << 36);
        Random rnd = new Random(seedLong);
        return StandardGeneratedPassword.generate(rnd, length);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int currentYear() {
        return Year.now().getValue();
    }
}
