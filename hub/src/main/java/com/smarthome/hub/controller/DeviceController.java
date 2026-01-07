package com.smarthome.hub.controller;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.dto.CreateDeviceRequest;
import com.smarthome.hub.dto.DeviceDto;
import com.smarthome.hub.dto.UpdateDeviceRequest;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.service.DeviceService;
import com.smarthome.hub.mqtt.MqttGateway;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
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
	public List<DeviceDto> list() {
		return deviceService.listDevices().stream().map(mapper::toDto).toList();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
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
}

