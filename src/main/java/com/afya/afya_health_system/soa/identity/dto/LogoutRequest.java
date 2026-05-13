package com.afya.afya_health_system.soa.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Optional payload for logout: target a single refresh session, or revoke every persisted refresh for the user.
 * When {@code revokeAllSessions} is null or true (default behaviour), every active refresh token for this user is revoked.
 * When {@code revokeAllSessions} is false, only {@link #refreshToken} is revoked once parsed and validated.
 * <p>Français : charge utile optionnelle de déconnexion — une session refresh précise ou toutes les sessions.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogoutRequest(String refreshToken, Boolean revokeAllSessions) {
}
