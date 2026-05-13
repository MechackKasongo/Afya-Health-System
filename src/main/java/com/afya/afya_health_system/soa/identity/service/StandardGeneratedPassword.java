package com.afya.afya_health_system.soa.identity.service;

import java.util.Locale;
import java.util.Random;

/**
 * Mot de passe généré : préfixe fixe {@code Hgrj} (H majuscule, grj minuscules), puis 2 chiffres,
 * exactement un caractère spécial (jeu aligné sur la validation {@code PasswordPolicy}), puis des
 * lettres aléatoires pour atteindre une longueur totale de 12 ou 16.
 */
final class StandardGeneratedPassword {

    private static final String PREFIX = "Hgrj";
    /** Même jeu que la validation {@code PasswordPolicy} pour les caractères spéciaux. */
    private static final char[] SPECIALS = "@#$%&*!?_+-.".toCharArray();
    private static final String LETTERS_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LETTERS_LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String LETTER_POOL = LETTERS_UPPER + LETTERS_LOWER;

    private StandardGeneratedPassword() {}

    static int normalizeLength(int length) {
        return (length == 12 || length == 16) ? length : 16;
    }

    static String generate(Random rnd, int length) {
        int len = normalizeLength(length);
        int letterCount = len - PREFIX.length() - 2 - 1;
        if (letterCount < 1) {
            throw new IllegalStateException("Longueur générée incohérente: " + len);
        }
        String twoDigits = String.format(Locale.ROOT, "%02d", rnd.nextInt(100));
        char special = SPECIALS[rnd.nextInt(SPECIALS.length)];
        StringBuilder sb = new StringBuilder(len);
        sb.append(PREFIX);
        sb.append(twoDigits);
        sb.append(special);
        for (int i = 0; i < letterCount; i++) {
            sb.append(LETTER_POOL.charAt(rnd.nextInt(LETTER_POOL.length())));
        }
        return sb.toString();
    }
}
