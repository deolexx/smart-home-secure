package com.smarthome.hub.config;

import com.smarthome.hub.domain.Role;
import com.smarthome.hub.domain.RoleName;
import com.smarthome.hub.domain.User;
import com.smarthome.hub.repository.RoleRepository;
import com.smarthome.hub.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Security-critical bootstrap:
 * - Ensures RBAC roles exist (ADMIN/USER/DEVICE).
 * - Optionally creates a dev admin for first run.
 *
 * STRIDE: mitigates Spoofing/EoP by guaranteeing role integrity at startup.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.bootstrap.admin.enabled:true}")
	private boolean adminEnabled;
	@Value("${app.bootstrap.admin.username:admin}")
	private String adminUsername;
	@Value("${app.bootstrap.admin.email:admin@local}")
	private String adminEmail;
	@Value("${app.bootstrap.admin.password:admin123!}")
	private String adminPassword;

	public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		for (RoleName name : RoleName.values()) {
			roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(null, name)));
		}

		if (!adminEnabled) {
			return;
		}

		if (userRepository.existsByUsername(adminUsername)) {
			return;
		}

		Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
		User admin = User.builder()
				.username(adminUsername)
				.email(adminEmail)
				.passwordHash(passwordEncoder.encode(adminPassword))
				.roles(Set.of(adminRole))
				.build();
		userRepository.save(admin);
		log.warn("BOOTSTRAP ADMIN CREATED: username='{}' password='{}' (dev only; change/remove before prod)",
				adminUsername, adminPassword);
	}
}

