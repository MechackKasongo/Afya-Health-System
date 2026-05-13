package com.afya.afya_health_system.soa.identity.service;

import com.afya.afya_health_system.soa.identity.config.PasswordPolicyProperties;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class StandardGeneratedPasswordTest {

    private static final String POLICY_SPECIALS = "@#$%&*!?_+-.";
    private static final String LETTER_POOL =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private static boolean isAllowedLetter(char c) {
        return LETTER_POOL.indexOf(c) >= 0;
    }

    private static int countPolicySpecials(String p) {
        int n = 0;
        for (int i = 0; i < p.length(); i++) {
            if (POLICY_SPECIALS.indexOf(p.charAt(i)) >= 0) {
                n++;
            }
        }
        return n;
    }

    @Test
    void length12_matchesShape() {
        Random rnd = new Random(999L);
        String p = StandardGeneratedPassword.generate(rnd, 12);
        assertThat(p).hasSize(12);
        assertThat(p).startsWith("Hgrj");
        assertThat(Character.isDigit(p.charAt(4))).isTrue();
        assertThat(Character.isDigit(p.charAt(5))).isTrue();
        assertThat(countPolicySpecials(p)).isEqualTo(1);
        assertThat(POLICY_SPECIALS.indexOf(p.charAt(6))).isGreaterThanOrEqualTo(0);
        for (int i = 7; i < 12; i++) {
            assertThat(isAllowedLetter(p.charAt(i))).isTrue();
        }
    }

    @Test
    void length16_matchesShape() {
        Random rnd = new Random(1001L);
        String p = StandardGeneratedPassword.generate(rnd, 16);
        assertThat(p).hasSize(16);
        assertThat(p).startsWith("Hgrj");
        assertThat(countPolicySpecials(p)).isEqualTo(1);
    }

    @Test
    void validatesAgainstPasswordPolicy() {
        PasswordPolicyProperties props = new PasswordPolicyProperties();
        props.setUserMinLength(12);
        props.setGeneratedMinLength(12);
        PasswordPolicy policy = new PasswordPolicy(props);
        Random rnd = new Random(4242L);
        policy.validateGenerated(StandardGeneratedPassword.generate(rnd, 12));
        rnd.setSeed(4243L);
        policy.validateGenerated(StandardGeneratedPassword.generate(rnd, 16));
    }

    @Test
    void memorableGenerator_stableForSameInputs() {
        String a = MemorablePasswordGenerator.generate("Jean", "Dupont", "", 2026, 5L, 12, 0);
        String b = MemorablePasswordGenerator.generate("Jean", "Dupont", "", 2026, 5L, 12, 0);
        assertThat(a).isEqualTo(b);
        assertThat(a).startsWith("Hgrj");
    }
}
