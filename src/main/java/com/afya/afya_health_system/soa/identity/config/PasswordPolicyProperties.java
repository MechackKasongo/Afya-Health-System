package com.afya.afya_health_system.soa.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Complexity rules for user-chosen passwords ({@link com.afya.afya_health_system.soa.identity.service.PasswordPolicy}).
 * Generated passwords use a shorter floor via {@link #generatedMinLength()}.
 */
@ConfigurationProperties(prefix = "app.security.password-policy")
public class PasswordPolicyProperties {

    /** Minimum length for passwords typed by users (bootstrap admin included). */
    private int userMinLength = 11;

    /** Longueur minimale pour les mots de passe générés automatiquement (format 12 ou 16 caractères). */
    private int generatedMinLength = 12;

    private boolean requireUppercase = true;

    private boolean requireLowercase = true;

    private boolean requireDigit = true;

    /** At least one symbol from an ASCII set (e.g. {@code @#$%&*!?}). */
    private boolean requireSpecial = true;

    public int getUserMinLength() {
        return userMinLength;
    }

    public void setUserMinLength(int userMinLength) {
        this.userMinLength = userMinLength;
    }

    public int getGeneratedMinLength() {
        return generatedMinLength;
    }

    public void setGeneratedMinLength(int generatedMinLength) {
        this.generatedMinLength = generatedMinLength;
    }

    public boolean isRequireUppercase() {
        return requireUppercase;
    }

    public void setRequireUppercase(boolean requireUppercase) {
        this.requireUppercase = requireUppercase;
    }

    public boolean isRequireLowercase() {
        return requireLowercase;
    }

    public void setRequireLowercase(boolean requireLowercase) {
        this.requireLowercase = requireLowercase;
    }

    public boolean isRequireDigit() {
        return requireDigit;
    }

    public void setRequireDigit(boolean requireDigit) {
        this.requireDigit = requireDigit;
    }

    public boolean isRequireSpecial() {
        return requireSpecial;
    }

    public void setRequireSpecial(boolean requireSpecial) {
        this.requireSpecial = requireSpecial;
    }
}
