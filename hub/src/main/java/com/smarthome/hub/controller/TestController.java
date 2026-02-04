package com.smarthome.hub.controller;

import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import com.smarthome.hub.dto.DeviceDto;
import com.smarthome.hub.dto.TestLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test", description = "Тестові ендпойнти з тестовою авторизацією")
public class TestController {

	private static final String TEST_USERNAME = "test";
	private static final String TEST_PASSWORD = "test123!";
	private static final String TEST_TOKEN = "test-token-123";

	@PostMapping("/auth/login")
	@Operation(summary = "Тестова авторизація", description = "Приймає username/password (test/test123!) і повертає тестовий bearer токен")
	public ResponseEntity<?> testLogin(@Valid @RequestBody TestLoginRequest request) {
		// Перевіряємо credentials
		if (TEST_USERNAME.equals(request.getUsername()) && TEST_PASSWORD.equals(request.getPassword())) {
			return ResponseEntity.ok(Map.of(
					"token", TEST_TOKEN,
					"tokenType", "Bearer",
					"message", "Тестова авторизація успішна. Використовуйте цей токен в заголовку Authorization: Bearer " + TEST_TOKEN
			));
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "Invalid credentials", "message", "Невірний username або password"));
		}
	}

	@GetMapping("/device")
	@Operation(
			summary = "Тестовий пристрій", 
			description = "Повертає замокані дані пристрою для тестування. Потребує авторизації через тестовий токен або JWT",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	public DeviceDto getMockDevice() {
		DeviceDto device = new DeviceDto();
		device.setId(1L);
		device.setName("Тестовий пристрій");
		device.setType(DeviceType.SENSOR);
		device.setStatus(DeviceStatus.ONLINE);
		device.setMqttClientId("test-device-001");
		device.setUpdatedAt(Instant.now());
		return device;
	}
}
