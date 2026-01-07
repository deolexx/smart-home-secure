package com.smarthome.hub.service;

import com.smarthome.hub.domain.Role;
import com.smarthome.hub.domain.RoleName;
import com.smarthome.hub.domain.User;
import com.smarthome.hub.dto.RegisterRequest;
import com.smarthome.hub.repository.RoleRepository;
import com.smarthome.hub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User registerUser(RegisterRequest request, boolean admin) {
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new IllegalArgumentException("Username already exists");
		}
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new IllegalArgumentException("Email already exists");
		}
		Role userRole = roleRepository.findByName(admin ? RoleName.ADMIN : RoleName.USER)
				.orElseThrow(() -> new IllegalStateException("Role not configured"));
		User user = User.builder()
				.username(request.getUsername())
				.email(request.getEmail())
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.roles(Set.of(userRole))
				.build();
		return userRepository.save(user);
	}
}

