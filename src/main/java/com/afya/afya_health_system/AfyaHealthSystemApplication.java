package com.afya.afya_health_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Excludes {@link UserDetailsServiceAutoConfiguration} so Spring Security does not create a development
 * {@code InMemoryUserDetailsManager} and log a random password; authentication is JWT-based only.
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class AfyaHealthSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(AfyaHealthSystemApplication.class, args);
	}

}
