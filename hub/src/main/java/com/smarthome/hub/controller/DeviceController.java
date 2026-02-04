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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
		return deviceService.listDevices().stream().map(mapper::toDto).toList();
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
	public DeviceDto get(@PathVariable Long id) {
		return mapper.toDto(deviceService.getDevice(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public DeviceDto update(@PathVariable Long id, @Valid @RequestBody UpdateDeviceRequest request) {
		return mapper.toDto(deviceService.updateDevice(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		deviceService.deleteDevice(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/commands")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> sendCommand(@PathVariable Long id, @RequestBody String commandJson) {
		var device = deviceService.getDevice(id);
		if (device.getMqttClientId() == null || device.getMqttClientId().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		mqttGateway.sendCommand(device.getMqttClientId(), commandJson);
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/{id}/temperature-unit")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Зміна одиниць температури", description = "Надсилає команду для зміни C/F через MQTT")
	@ApiResponses({
		@ApiResponse(responseCode = "202", description = "Command accepted"),
		@ApiResponse(responseCode = "400", description = "Device has no MQTT client id or invalid request")
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
	public ResponseEntity<Void> setTemperatureUnit(@PathVariable Long id, @Valid @RequestBody TemperatureUnitCommandRequest request) {
		var device = deviceService.getDevice(id);
		if (device.getMqttClientId() == null || device.getMqttClientId().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		String unit = request.getUnit().trim().toUpperCase();
		String commandJson = String.format("{\"unit\":\"%s\"}", unit);
		mqttGateway.sendCommand(device.getMqttClientId(), commandJson);
		return ResponseEntity.accepted().build();
	}
}

