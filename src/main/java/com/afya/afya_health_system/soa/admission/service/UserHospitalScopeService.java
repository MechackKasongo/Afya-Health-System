package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.model.Admission;
import com.afya.afya_health_system.soa.common.constants.HospitalCatalogConstants;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.identity.model.AppUser;
import com.afya.afya_health_system.soa.identity.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves per-user hospital service scope for admission list, urgences, and resource access.
 * <p>Français : périmètre des services hospitaliers par utilisateur (admissions, urgences, etc.).</p>
 */
@Service
public class UserHospitalScopeService {

    private static final Logger log = LoggerFactory.getLogger(UserHospitalScopeService.class);

    private final AppUserRepository appUserRepository;

    public UserHospitalScopeService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    /**
     * When empty optional, the caller should not apply a service filter.
     * When present, the set is non-empty and limits visibility to these service names.
     */
    public Optional<Set<String>> restrictedServiceNames(String username) {
        AppUser user = appUserRepository.findByUsernameWithHospitalServices(username)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable: " + username));
        Set<String> roles = user.rolesAsSet();
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_RECEPTION")) {
            return Optional.empty();
        }
        if (!roles.contains("ROLE_MEDECIN") && !roles.contains("ROLE_INFIRMIER")) {
            return Optional.empty();
        }
        if (user.getHospitalServices() == null || user.getHospitalServices().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(user.getHospitalServices().stream()
                .map(h -> h.getName())
                .collect(Collectors.toSet()));
    }

    public void assertCanAccessAdmission(String username, Admission admission) {
        restrictedServiceNames(username).ifPresent(allowed -> {
            String sn = admission.getServiceName();
            if (sn == null || allowed.stream().noneMatch(a -> a.equalsIgnoreCase(sn.trim()))) {
                throw new NotFoundException("Admission introuvable: " + admission.getId());
            }
        });
    }

    public void assertCanUseServiceName(String username, String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            throw new ConflictException("Service hospitalier invalide");
        }
        String trimmed = serviceName.trim();
        restrictedServiceNames(username).ifPresent(allowed -> {
            if (allowed.stream().noneMatch(a -> a.equalsIgnoreCase(trimmed))) {
                throw new ConflictException("Service non autorisé pour votre affectation.");
            }
        });
    }

    /**
     * Accès au module urgences (liste, détail, actions) si le périmètre est ouvert ou si le service catalogue
     * {@link com.afya.afya_health_system.soa.common.constants.HospitalCatalogConstants#URGENCES_SERVICE_NAME} est affecté.
     */
    public boolean hasUrgencesAccess(String username) {
        Optional<Set<String>> restricted = restrictedServiceNames(username);
        if (restricted.isEmpty()) {
            return true;
        }
        return restricted.get().stream()
                .anyMatch(a -> HospitalCatalogConstants.URGENCES_SERVICE_NAME.equalsIgnoreCase(a.trim()));
    }

    /**
     * {@code true} when the user has a restricted clinical scope that excludes « Urgences » — the urgences list is intentionally empty for them.
     */
    public boolean isUrgencesListHiddenByAssignment(String username) {
        Optional<Set<String>> restricted = restrictedServiceNames(username);
        return restricted.isPresent() && !hasUrgencesAccess(username);
    }

    /**
     * Création / mutation urgences : même règle que {@link #hasUrgencesAccess(String)} avec erreur explicite si refus.
     */
    public void assertCanUseUrgences(String username) {
        restrictedServiceNames(username).ifPresent(allowed -> {
            boolean ok = allowed.stream()
                    .anyMatch(a -> HospitalCatalogConstants.URGENCES_SERVICE_NAME.equalsIgnoreCase(a.trim()));
            if (!ok) {
                log.warn("Urgences access denied (CREATE): user={}", username);
                throw new ConflictException("Accès aux urgences non autorisé pour votre affectation.");
            }
        });
    }
}
