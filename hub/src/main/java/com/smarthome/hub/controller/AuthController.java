package com.smarthome.hub.controller;

import com.smarthome.hub.dto.RegisterRequest;
import com.smarthome.hub.service.KeycloakAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

	public AuthController(KeycloakAdminService keycloakAdminService) {
		this.keycloakAdminService = keycloakAdminService;
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

