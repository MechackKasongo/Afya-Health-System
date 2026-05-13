package com.afya.afya_health_system.soa.identity.service;

import com.afya.afya_health_system.soa.common.exception.BadRequestException;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.identity.dto.PasswordPreviewRequest;
import com.afya.afya_health_system.soa.identity.dto.PasswordPreviewResponse;
import com.afya.afya_health_system.soa.identity.dto.RoleOptionResponse;
import com.afya.afya_health_system.soa.identity.dto.UserCreateRequest;
import com.afya.afya_health_system.soa.identity.dto.UserResponse;
import com.afya.afya_health_system.soa.identity.dto.UserStatusUpdateRequest;
import com.afya.afya_health_system.soa.identity.dto.UserUpdateRequest;
import com.afya.afya_health_system.soa.identity.model.AppUser;
import com.afya.afya_health_system.soa.identity.model.Role;
import com.afya.afya_health_system.soa.identity.repository.AppUserRepository;
import com.afya.afya_health_system.soa.identity.repository.RoleRepository;
import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import com.afya.afya_health_system.soa.hospitalservice.repository.HospitalServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserManagementService {
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final HospitalServiceRepository hospitalServiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCredentialsFileService userCredentialsFileService;
    private final PasswordPolicy passwordPolicy;

    public UserManagementService(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            HospitalServiceRepository hospitalServiceRepository,
            PasswordEncoder passwordEncoder,
            UserCredentialsFileService userCredentialsFileService,
            PasswordPolicy passwordPolicy
    ) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCredentialsFileService = userCredentialsFileService;
        this.passwordPolicy = passwordPolicy;
    }

    public Page<UserResponse> list(String query, Integer page, Integer size, String sortBy, String sortDir) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 20 : size;
        Sort sort = buildSafeSort(sortBy, sortDir);
        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);

        if (query == null || query.isBlank()) {
            return appUserRepository.findAll(pageable).map(this::toResponse);
        }
        return appUserRepository
                .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query.trim(),
                        query.trim(),
                        query.trim(),
                        pageable
                )
                .map(this::toResponse);
    }

    public UserResponse create(UserCreateRequest request) {
        String first = trimToNull(request.firstName());
        String last = trimToNull(request.lastName());
        String post = trimToNull(request.postName());
        final String username;
        final String fullName;
        final boolean structuredNames = first != null && last != null;

        if (structuredNames) {
            String base = UsernameSlug.combineFirstLast(first, last);
            username = uniqueUsername(base);
            fullName = buildDisplayFullName(first, last, post);
        } else if (trimToNull(request.username()) != null && trimToNull(request.fullName()) != null) {
            username = request.username().trim();
            fullName = request.fullName().trim();
        } else {
            throw new BadRequestException("Renseignez le prénom et le nom, ou le nom d'utilisateur et le nom complet.");
        }

        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException("Nom d'utilisateur déjà existant");
        }
        if (request.email() != null && !request.email().isBlank() && appUserRepository.existsByEmail(request.email().trim())) {
            throw new ConflictException("Adresse email déjà utilisée");
        }
        String plainPassword;
        if (request.password() != null && !request.password().isBlank()) {
            passwordPolicy.validateUserChosen(request.password());
            plainPassword = request.password();
        } else {
            int length = resolveGeneratedLength(request.generatedPasswordLength());
            long ordinal = appUserRepository.findMaxId() + 1;
            int year = SecurePasswordGenerator.currentYear();
            if (structuredNames) {
                plainPassword = MemorablePasswordGenerator.generate(
                        first,
                        last,
                        post != null ? post : "",
                        year,
                        ordinal,
                        length,
                        Math.max(0, request.passwordVariation() == null ? 0 : request.passwordVariation())
                );
            } else {
                plainPassword = SecurePasswordGenerator.generateFromIdentity(
                        fullName,
                        username,
                        year,
                        ordinal,
                        length
                );
            }
        }
        passwordPolicy.validateGenerated(plainPassword);
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setFullName(fullName);
        u.setEmail(normalizeEmail(request.email()));
        u.setPasswordHash(passwordEncoder.encode(plainPassword));
        u.setRoles(Set.of(resolveRole(request.role())));
        applyHospitalServices(u, request.hospitalServiceIds(), true);
        u.setActive(true);
        AppUser saved = appUserRepository.save(u);
        userCredentialsFileService.appendEntry(saved.getUsername(), saved.getFullName(), plainPassword);
        return toResponse(saved, plainPassword);
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = findEntity(id);
        user.setFullName(request.fullName().trim());
        String normalizedEmail = normalizeEmail(request.email());
        if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(user.getEmail()) && appUserRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Adresse email déjà utilisée");
        }
        user.setEmail(normalizedEmail);
        user.setRoles(Set.of(resolveRole(request.role())));
        applyHospitalServices(user, request.hospitalServiceIds(), false);
        if (request.password() != null && !request.password().isBlank()) {
            String pwd = request.password().strip();
            passwordPolicy.validateUserChosen(pwd);
            user.setPasswordHash(passwordEncoder.encode(pwd));
        }
        return toResponse(appUserRepository.save(user));
    }

    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        AppUser user = findEntity(id);
        if (!request.active() && isActiveAdmin(user) && countActiveAdmins() <= 1) {
            throw new ConflictException("Impossible de désactiver le dernier administrateur actif");
        }
        user.setActive(request.active());
        return toResponse(appUserRepository.save(user));
    }

    public void delete(Long id, String currentUsername) {
        AppUser user = findEntity(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new ConflictException("Auto-suppression interdite");
        }
        if (isActiveAdmin(user) && countActiveAdmins() <= 1) {
            throw new ConflictException("Impossible de supprimer le dernier administrateur actif");
        }
        appUserRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<RoleOptionResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleOptionResponse(r.getId(), r.getCode(), r.getLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PasswordPreviewResponse previewPassword(PasswordPreviewRequest request) {
        String first = request.firstName().trim();
        String last = request.lastName().trim();
        String post = request.postName() == null || request.postName().isBlank() ? "" : request.postName().trim();
        int length = resolveGeneratedLength(request.generatedPasswordLength());
        long ordinal = appUserRepository.findMaxId() + 1;
        int year = SecurePasswordGenerator.currentYear();
        int variation = Math.max(0, request.variation() == null ? 0 : request.variation());
        String password = MemorablePasswordGenerator.generate(first, last, post, year, ordinal, length, variation);
        return new PasswordPreviewResponse(password);
    }

    private AppUser findEntity(Long id) {
        return appUserRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable: " + id));
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private String uniqueUsername(String base) {
        String trimmed = base.length() > 80 ? base.substring(0, 80) : base;
        String candidate = trimmed;
        int n = 2;
        while (appUserRepository.existsByUsername(candidate)) {
            String suffix = String.valueOf(n++);
            candidate = trimmed.length() + suffix.length() > 80
                    ? trimmed.substring(0, 80 - suffix.length()) + suffix
                    : trimmed + suffix;
            if (n > 100_000) {
                throw new ConflictException("Impossible de générer un nom d'utilisateur unique.");
            }
        }
        return candidate;
    }

    private static String buildDisplayFullName(String first, String last, String post) {
        String a = capitalizeWord(first);
        String b = capitalizeWord(last);
        String c = post != null ? capitalizeWord(post) : "";
        if (c.isEmpty()) {
            return (a + " " + b).trim();
        }
        return (a + " " + b + " " + c).trim();
    }

    private static String capitalizeWord(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        if (t.length() == 1) {
            return t.toUpperCase();
        }
        return t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
    }

    private Role resolveRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new BadRequestException("Un rôle est requis");
        }
        return roleRepository.findByCode(roleName.trim())
                .orElseThrow(() -> new BadRequestException("Rôle invalide: " + roleName));
    }

    private static int resolveGeneratedLength(Integer requested) {
        if (requested == null) {
            return 16;
        }
        if (requested == 12 || requested == 16) {
            return requested;
        }
        throw new BadRequestException("Longueur du mot de passe généré : choisir 12 ou 16.");
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private Sort buildSafeSort(String sortBy, String sortDir) {
        Set<String> allowed = Set.of("id", "username", "fullName", "active");
        String field = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!allowed.contains(field)) field = "id";

        boolean desc = sortDir == null || sortDir.isBlank() || sortDir.equalsIgnoreCase("desc");
        Sort.Direction dir = desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }

    private boolean isActiveAdmin(AppUser user) {
        return user.isActive() && user.rolesAsSet().contains("ROLE_ADMIN");
    }

    private long countActiveAdmins() {
        return appUserRepository.findAll().stream()
                .filter(this::isActiveAdmin)
                .count();
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.rolesAsSet(),
                user.isActive(),
                sortedHospitalServiceIds(user),
                null
        );
    }

    private UserResponse toResponse(AppUser user, String generatedPassword) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.rolesAsSet(),
                user.isActive(),
                sortedHospitalServiceIds(user),
                generatedPassword
        );
    }

    private static List<Long> sortedHospitalServiceIds(AppUser user) {
        if (user.getHospitalServices() == null || user.getHospitalServices().isEmpty()) {
            return List.of();
        }
        return user.getHospitalServices().stream().map(HospitalService::getId).sorted().toList();
    }

    private void applyHospitalServices(AppUser u, List<Long> ids, boolean isCreate) {
        if (!isCreate && ids == null) {
            return;
        }
        List<Long> effective = ids == null ? List.of() : ids;
        if (effective.isEmpty()) {
            u.setHospitalServices(new HashSet<>());
            return;
        }
        List<HospitalService> found = hospitalServiceRepository.findAllById(effective);
        if (found.size() != effective.size()) {
            throw new BadRequestException("Identifiant de service hospitalier invalide.");
        }
        u.setHospitalServices(new HashSet<>(found));
    }
}
