package com.smarthome.hub.controller;

import com.smarthome.hub.dto.LoginRequest;
import com.smarthome.hub.dto.RegisterRequest;
import com.smarthome.hub.service.KeycloakTokenService;
import com.smarthome.hub.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Auth endpoints for user registration.
 * 
 * For Keycloak authentication, use:
 * - Login: POST http://localhost:8090/realms/smarthome/protocol/openid-connect/token
 * - Or use Keycloak Admin Console: http://localhost:8090
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API для реєстрації користувачів та отримання інформації про аутентифікацію")
public class AuthController {

	private final KeycloakAdminService keycloakAdminService;
	private final KeycloakTokenService keycloakTokenService;

	public AuthController(KeycloakAdminService keycloakAdminService, KeycloakTokenService keycloakTokenService) {
		this.keycloakAdminService = keycloakAdminService;
		this.keycloakTokenService = keycloakTokenService;
	}

	@PostMapping("/register")
	@Operation(summary = "Реєстрація нового користувача", description = "Створює нового користувача в Keycloak з роллю USER")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "Користувач успішно зареєстрований"),
			@ApiResponse(responseCode = "400", description = "Невірні дані (користувач з таким username або email вже існує)"),
			@ApiResponse(responseCode = "500", description = "Помилка сервера")
	})
	public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
		try {
			keycloakAdminService.registerUser(request, false);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(new RegisterResponse("User registered successfully"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ErrorResponse(e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse(e.getMessage()));
		}
	}

	@PostMapping("/token")
	@Operation(summary = "Отримання токена", description = "Отримує access token через Keycloak (password grant)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Токен успішно отриманий"),
			@ApiResponse(responseCode = "401", description = "Невірні облікові дані"),
			@ApiResponse(responseCode = "500", description = "Помилка сервера")
	})
	public ResponseEntity<?> token(@Valid @RequestBody LoginRequest request) {
		try {
			return ResponseEntity.ok(keycloakTokenService.login(request.getUsername(), request.getPassword()));
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new ErrorResponse("Invalid credentials"));
		} catch (IllegalStateException ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse(ex.getMessage()));
		}
	}

	@GetMapping("/info")
	@Operation(summary = "Інформація про аутентифікацію", description = "Повертає інформацію про Keycloak endpoints для аутентифікації")
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

	public record RegisterResponse(String message) {}

	public record ErrorResponse(String error) {}
}

