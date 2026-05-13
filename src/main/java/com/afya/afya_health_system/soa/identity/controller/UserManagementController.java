package com.afya.afya_health_system.soa.identity.controller;

import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.identity.dto.CredentialsLogPreviewResponse;
import com.afya.afya_health_system.soa.identity.dto.PasswordPreviewRequest;
import com.afya.afya_health_system.soa.identity.dto.PasswordPreviewResponse;
import com.afya.afya_health_system.soa.identity.dto.RoleOptionResponse;
import com.afya.afya_health_system.soa.identity.dto.UserCreateRequest;
import com.afya.afya_health_system.soa.identity.dto.UserResponse;
import com.afya.afya_health_system.soa.identity.dto.UserStatusUpdateRequest;
import com.afya.afya_health_system.soa.identity.dto.UserUpdateRequest;
import com.afya.afya_health_system.soa.identity.service.UserCredentialsFileService;
import com.afya.afya_health_system.soa.identity.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {
    private final UserManagementService userManagementService;
    private final UserCredentialsFileService userCredentialsFileService;

    public UserManagementController(
            UserManagementService userManagementService,
            UserCredentialsFileService userCredentialsFileService
    ) {
        this.userManagementService = userManagementService;
        this.userCredentialsFileService = userCredentialsFileService;
    }

    @GetMapping
    public Page<UserResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return userManagementService.list(query, page, size, sortBy, sortDir);
    }

    @GetMapping("/roles")
    public List<RoleOptionResponse> roles() {
        return userManagementService.listRoles();
    }

    /**
     * Preview for UI (scroll + actions: download / delete / print).
     */
    @GetMapping("/credentials-log/preview")
    public CredentialsLogPreviewResponse previewCredentialsLog(
            @RequestParam(name = "maxBytes", required = false, defaultValue = "65536") int maxBytes
    ) {
        int capped = Math.min(Math.max(maxBytes, 4096), 512_000);
        return userCredentialsFileService.buildPreview(capped);
    }

    /**
     * Remove the credential log file from disk (admin only).
     */
    @DeleteMapping("/credentials-log")
    public ResponseEntity<Void> deleteCredentialsLog() {
        userCredentialsFileService.deleteLog();
        return ResponseEntity.noContent().build();
    }

    /**
     * Download the server-side TSV file of generated usernames and plaintext passwords (admin only).
     */
    @GetMapping("/credentials-log")
    public ResponseEntity<Resource> downloadCredentialsLog() {
        if (!userCredentialsFileService.logFileExists()) {
            throw new NotFoundException("Aucun fichier de comptes généré pour le moment.");
        }
        Resource body = userCredentialsFileService.asDownloadableResource();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comptes-utilisateurs-afya.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    @GetMapping("/credentials-log.csv")
    public ResponseEntity<Resource> downloadCredentialsCsv() {
        if (!userCredentialsFileService.csvLogExists()) {
            throw new NotFoundException("Aucun fichier CSV de comptes pour le moment.");
        }
        Resource body = userCredentialsFileService.asCsvDownloadableResource();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comptes-utilisateurs-afya.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    @PostMapping("/password-preview")
    public PasswordPreviewResponse previewPassword(@Valid @RequestBody PasswordPreviewRequest body) {
        return userManagementService.previewPassword(body);
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userManagementService.create(request);
    }

    @PutMapping("/{id:\\d+}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userManagementService.update(id, request);
    }

    @PatchMapping("/{id:\\d+}/status")
    public UserResponse updateStatus(@PathVariable Long id, @RequestBody UserStatusUpdateRequest request) {
        return userManagementService.updateStatus(id, request);
    }

    @DeleteMapping("/{id:\\d+}")
    public void delete(@PathVariable Long id, Authentication authentication) {
        userManagementService.delete(id, authentication.getName());
    }
}
