package com.afya.afya_health_system.soa.identity.config;

import com.afya.afya_health_system.soa.identity.model.AppUser;
import com.afya.afya_health_system.soa.identity.model.Role;
import com.afya.afya_health_system.soa.identity.repository.AppUserRepository;
import com.afya.afya_health_system.soa.identity.repository.RoleRepository;
import com.afya.afya_health_system.soa.identity.service.PasswordPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Creates catalog roles and a first administrator from {@code app.bootstrap.*} when the database is empty of that account.
 * Disable with {@code app.bootstrap.auto-provision=false} once users are seeded by migrations or DBA.
 * <p>Français : provisionne rôles + compte initial depuis {@code app.bootstrap.*}; désactiver en prod après import SQL.</p>
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.auto-provision", havingValue = "true", matchIfMissing = true)
public class BootstrapAccountProvisioner implements ApplicationRunner {

    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordPolicy passwordPolicy;
    private final String bootstrapUsername;
    private final String bootstrapFullName;
    private final String bootstrapPassword;
    private final Set<String> bootstrapRoles;

    public BootstrapAccountProvisioner(
            PasswordEncoder passwordEncoder,
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PasswordPolicy passwordPolicy,
            @Value("${app.bootstrap.username}") String bootstrapUsername,
            @Value("${app.bootstrap.full-name}") String bootstrapFullName,
            @Value("${app.bootstrap.password}") String bootstrapPassword,
            @Value("${app.bootstrap.roles:ROLE_ADMIN}") String bootstrapRoles
    ) {
        this.passwordEncoder = passwordEncoder;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.passwordPolicy = passwordPolicy;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapFullName = bootstrapFullName;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapRoles = Arrays.stream(bootstrapRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            throw new IllegalStateException(
                    "Configuration invalide : app.bootstrap.username et app.bootstrap.password sont requis lorsque "
                            + "app.bootstrap.auto-provision=true (profiles sans valeurs par défaut)."
            );
        }
        ensureRoles();
        if (appUserRepository.existsByUsername(bootstrapUsername.trim())) {
            return;
        }
        passwordPolicy.validateUserChosen(bootstrapPassword);
        AppUser user = new AppUser();
        user.setUsername(bootstrapUsername.trim());
        user.setFullName(bootstrapFullName != null ? bootstrapFullName.strip() : "Administrateur");
        user.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        user.setRoles(resolveRoles(bootstrapRoles));
        appUserRepository.save(user);
    }

    private void ensureRoles() {
        createRoleIfMissing("ROLE_ADMIN", "Admin");
        createRoleIfMissing("ROLE_MEDECIN", "Médecin");
        createRoleIfMissing("ROLE_INFIRMIER", "Infirmier(ère)");
        createRoleIfMissing("ROLE_RECEPTION", "Réceptionniste");
    }

    private void createRoleIfMissing(String roleName, String label) {
        if (roleRepository.findByCode(roleName).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setCode(roleName);
        role.setLabel(label);
        roleRepository.save(role);
    }

    private Set<Role> resolveRoles(Set<String> names) {
        return names.stream()
                .map(name -> roleRepository.findByCode(name)
                        .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Rôle introuvable: " + name)))
                .collect(Collectors.toSet());
    }
}
