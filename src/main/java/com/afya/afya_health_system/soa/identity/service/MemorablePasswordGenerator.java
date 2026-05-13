package com.afya.afya_health_system.soa.identity.service;

import java.util.Random;

/**
 * Génère un mot de passe au format {@link StandardGeneratedPassword}, avec un {@link Random} déterministe
 * dérivé du prénom, nom, post-nom, année, ordinal et variation (aperçu reproductible).
 */
public final class MemorablePasswordGenerator {

    private MemorablePasswordGenerator() {}

    /**
     * @param length 12 ou 16
     * @param variation incrémenter pour une autre proposition (même graine logique qu’à la création).
     */
    public static String generate(
            String firstName,
            String lastName,
            String postName,
            int yearFull,
            long ordinal,
            int length,
            int variation
    ) {
        long seed = (long) variation * 0x9E3779B97F4A7C15L
                ^ ordinal * 0x85EBCA6BL
                ^ ((long) yearFull << 20)
                ^ ((long) firstName.hashCode() << 11)
                ^ ((long) lastName.hashCode() << 3)
                ^ (long) (postName == null ? 0 : postName.hashCode());
        Random rnd = new Random(seed);
        return StandardGeneratedPassword.generate(rnd, length);
    }
}
