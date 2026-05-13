package com.afya.afya_health_system.soa.identity.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Builds a lowercase ASCII username fragment from a human name (letters only).
 */
public final class UsernameSlug {

    private UsernameSlug() {}

    public static String slug(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String n = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder sb = new StringBuilder();
        for (char c : n.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static String combineFirstLast(String firstName, String lastName) {
        String a = slug(firstName);
        String b = slug(lastName);
        String joined = a + b;
        if (joined.isEmpty()) {
            return "user";
        }
        return joined.toLowerCase(Locale.ROOT);
    }
}
