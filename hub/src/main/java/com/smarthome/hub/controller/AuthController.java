package com.smarthome.hub.controller;

import com.smarthome.hub.domain.User;
import com.smarthome.hub.dto.JwtResponse;
import com.smarthome.hub.dto.LoginRequest;
import com.smarthome.hub.dto.RegisterRequest;
import com.smarthome.hub.security.JwtTokenService;
import com.smarthome.hub.service.UserService;
import com.smarthome.hub.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final UserService userService;
	private final UserRepository userRepository;

	public AuthController(AuthenticationManager authenticationManager,
	                      JwtTokenService jwtTokenService,
	                      UserService userService,
	                      UserRepository userRepository) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
		this.userService = userService;
		this.userRepository = userRepository;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
		User user = userService.registerUser(request, false);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
		String token = jwtTokenService.generateToken(user);
		return ResponseEntity.ok(new JwtResponse(token));
	}
}

