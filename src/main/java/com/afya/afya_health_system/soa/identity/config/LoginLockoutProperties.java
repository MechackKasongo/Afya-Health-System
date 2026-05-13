package com.afya.afya_health_system.soa.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Account lockout after repeated failed password attempts (per existing {@link com.afya.afya_health_system.soa.identity.model.AppUser} row).
 * Independent of per-IP login throttling.
 * <p>Français : verrouillage du compte après mauvais mot de passe répétés, en base (pas seulement par IP).</p>
 */
@ConfigurationProperties(prefix = "app.security.login-lockout")
public class LoginLockoutProperties {

    private boolean enabled = true;

    /** Lock after this many consecutive wrong passwords (for a known username). */
    private int maxAttempts = 8;

    /** How long to refuse logins after the threshold is reached. */
    private int lockDurationMinutes = 15;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(int lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }
}
