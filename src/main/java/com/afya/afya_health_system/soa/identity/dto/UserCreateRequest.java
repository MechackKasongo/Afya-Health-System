package com.afya.afya_health_system.soa.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        /** Optionnel si {@code firstName} et {@code lastName} sont fournis (username généré). */
        @Size(max = 80) String username,
        /** Optionnel si prénom/nom fournis (nom complet construit côté serveur). */
        @Size(max = 120) String fullName,
        @Email @Size(max = 160) String email,
        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Size(max = 60) String postName,
        /** Si null ou blank, mot de passe généré (voir {@code generatedPasswordLength}). */
        @Size(max = 200) String password,
        /** Mot de passe auto : 12 ou 16 (défaut 16). Ignoré si mot de passe manuel. */
        Integer generatedPasswordLength,
        /** Variation pour retrouver la même proposition qu'à l'aperçu (icône régénérer). */
        Integer passwordVariation,
        @NotBlank @Size(max = 80) String role,
        /** Services hospitaliers affectés (IDs catalogue) ; vide ou absent = aucune affectation. */
        List<Long> hospitalServiceIds
) {}
