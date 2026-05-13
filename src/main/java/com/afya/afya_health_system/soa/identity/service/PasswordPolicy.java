package com.afya.afya_health_system.soa.identity.service;

import com.afya.afya_health_system.soa.common.exception.BadRequestException;
import com.afya.afya_health_system.soa.identity.config.PasswordPolicyProperties;
import org.springframework.stereotype.Component;

/**
 * Enforces configurable password complexity for human-chosen secrets and a relaxed floor for generator output.
 */
@Component
public class PasswordPolicy {

    private static final String SPECIAL_CLASS = "@#$%&*!?_-+.";

    private final PasswordPolicyProperties properties;

    public PasswordPolicy(PasswordPolicyProperties properties) {
        this.properties = properties;
    }

    /** Password entered by an administrator or user (stricter minimum length). */
    public void validateUserChosen(String password) {
        validate(password, properties.getUserMinLength());
    }

    /** Mot de passe généré (plancher distinct du mot de passe saisi, ex. 12 caractères). */
    public void validateGenerated(String password) {
        validate(password, properties.getGeneratedMinLength());
    }

    private void validate(String password, int minLength) {
        if (password == null) {
            throw new BadRequestException("Le mot de passe est requis.");
        }
        String p = password;
        if (p.length() < minLength) {
            throw new BadRequestException("Le mot de passe doit contenir au moins " + minLength + " caractères.");
        }
        if (properties.isRequireUppercase() && p.chars().noneMatch(Character::isUpperCase)) {
            throw new BadRequestException("Le mot de passe doit contenir au moins une majuscule.");
        }
        if (properties.isRequireLowercase() && p.chars().noneMatch(Character::isLowerCase)) {
            throw new BadRequestException("Le mot de passe doit contenir au moins une minuscule.");
        }
        if (properties.isRequireDigit() && p.chars().noneMatch(Character::isDigit)) {
            throw new BadRequestException("Le mot de passe doit contenir au moins un chiffre.");
        }
        if (properties.isRequireSpecial()) {
            boolean hasSpecial = false;
            for (int i = 0; i < p.length(); i++) {
                if (SPECIAL_CLASS.indexOf(p.charAt(i)) >= 0) {
                    hasSpecial = true;
                    break;
                }
            }
            if (!hasSpecial) {
                throw new BadRequestException(
                        "Le mot de passe doit contenir au moins un caractère spécial parmi : " + SPECIAL_CLASS
                );
            }
        }
    }
}
