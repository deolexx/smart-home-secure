package com.smarthome.hub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints are now handled by Keycloak.
 * 
 * For registration and login, use Keycloak endpoints:
 * - Registration: POST http://localhost:8090/realms/smarthome/registrations
 * - Login: POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token
 * 
 * Or use Keycloak Admin Console: http://localhost:8090
 * 
 * This controller provides information about Keycloak endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@GetMapping("/info")
	public ResponseEntity<?> authInfo() {
		return ResponseEntity.ok(new AuthInfo(
				"http://localhost:8090/realms/smarthome/protocol/openid-connect/token",
				"http://localhost:8090/realms/smarthome/protocol/openid-connect/auth",
				"http://localhost:8090/realms/smarthome/protocol/openid-connect/userinfo",
				"Use Keycloak Admin Console at http://localhost:8090 for user management"
		));
	}

	public record AuthInfo(
			String tokenEndpoint,
			String authorizationEndpoint,
			String userInfoEndpoint,
			String adminConsole
	) {}
}

