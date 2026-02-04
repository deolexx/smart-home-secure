package com.smarthome.hub.controller;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.dto.CreateDeviceRequest;
import com.smarthome.hub.dto.DeviceDto;
import com.smarthome.hub.dto.UpdateDeviceRequest;
import com.smarthome.hub.dto.TemperatureUnitCommandRequest;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.service.DeviceService;
import com.smarthome.hub.mqtt.MqttGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/devices")
@Tag(name = "Devices", description = "API для управління IoT пристроями")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

	private final DeviceService deviceService;
	private final DeviceMapper mapper;
	private final MqttGateway mqttGateway;

	public DeviceController(DeviceService deviceService, DeviceMapper mapper, MqttGateway mqttGateway) {
		this.deviceService = deviceService;
		this.mapper = mapper;
		this.mqttGateway = mqttGateway;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	@Operation(summary = "Список пристроїв", description = "Отримує список всіх пристроїв. Доступно для ADMIN та USER")
	public List<DeviceDto> list() {
		Authentication auth = currentAuth();
		return deviceService.listDevicesForUser(currentUserId(auth), isAdmin(auth))
				.stream()
				.map(mapper::toDto)
				.toList();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Створення пристрою", description = "Створює новий пристрій. Тільки для ADMIN")
	public ResponseEntity<DeviceDto> create(@Valid @RequestBody CreateDeviceRequest request) {
		Device created = deviceService.createDevice(request);
		return ResponseEntity.created(URI.create("/api/devices/" + created.getId())).body(mapper.toDto(created));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public DeviceDto get(@PathVariable UUID id) {
		Authentication auth = currentAuth();
		return mapper.toDto(deviceService.getDeviceForUser(id, currentUserId(auth), isAdmin(auth)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public DeviceDto update(@PathVariable UUID id, @Valid @RequestBody UpdateDeviceRequest request) {
		return mapper.toDto(deviceService.updateDevice(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		deviceService.deleteDevice(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/commands")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> sendCommand(@PathVariable UUID id, @RequestBody String commandJson) {
		var device = deviceService.getDevice(id);
		if (device.getMqttClientId() == null || device.getMqttClientId().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		mqttGateway.sendCommand(device.getMqttClientId(), commandJson);
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/by-client/{clientId}/temperature-unit")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Зміна одиниць температури (MQTT client id)", description = "Надсилає команду для зміни C/F за mqttClientId")
	@ApiResponses({
		@ApiResponse(responseCode = "202", description = "Command accepted"),
		@ApiResponse(responseCode = "400", description = "Invalid request")
	})
	@RequestBody(
		description = "Temperature unit command",
		required = true,
		content = @Content(
			schema = @Schema(implementation = TemperatureUnitCommandRequest.class),
			examples = {
				@ExampleObject(name = "Celsius", value = "{\"unit\":\"C\"}"),
				@ExampleObject(name = "Fahrenheit", value = "{\"unit\":\"F\"}")
			}
		)
	)
	public ResponseEntity<Void> setTemperatureUnitByClientId(@PathVariable UUID clientId,
	                                                         @Valid @RequestBody(required = false) TemperatureUnitCommandRequest request,
	                                                         @RequestParam(name = "unit", required = false) String unitParam) {
		var device = deviceService.getDeviceByClientId(clientId.toString());
		if (device.getMqttClientId() == null || device.getMqttClientId().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		String unit = extractUnit(request, unitParam);
		if (unit == null || unit.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		mqttGateway.sendCommand(device.getMqttClientId(), temperatureUnitCommand(unit));
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/{id}/claim")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	@Operation(summary = "Прив'язати пристрій до користувача", description = "Користувач може забрати вільний пристрій")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Device claimed"),
		@ApiResponse(responseCode = "409", description = "Device already claimed")
	})
	public DeviceDto claim(@PathVariable UUID id) {
		Authentication auth = currentAuth();
		try {
			return mapper.toDto(deviceService.claimDevice(id, currentUserId(auth)));
		} catch (IllegalStateException ex) {
			throw new ResponseStatusException(CONFLICT, ex.getMessage());
		}
	}

	private String temperatureUnitCommand(String unit) {
		String normalized = unit.trim().toUpperCase();
		if (!"C".equals(normalized) && !"F".equals(normalized)) {
			throw new IllegalArgumentException("unit must be C or F");
		}
		return String.format("{\"unit\":\"%s\"}", normalized);
	}

	private String extractUnit(TemperatureUnitCommandRequest request, String unitParam) {
		if (request != null && request.getUnit() != null) {
			return request.getUnit();
		}
		return unitParam;
	}

	private Authentication currentAuth() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			throw new ResponseStatusException(FORBIDDEN, "Access denied");
		}
		return auth;
	}

	private boolean isAdmin(Authentication auth) {
		return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}

	private String currentUserId(Authentication auth) {
		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			return jwtAuth.getToken().getSubject();
		}
		return auth.getName();
	}
}

