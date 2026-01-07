package com.smarthome.hub.controller;

import com.smarthome.hub.domain.DeviceTelemetry;
import com.smarthome.hub.service.TelemetryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

	private final TelemetryService telemetryService;

	public TelemetryController(TelemetryService telemetryService) {
		this.telemetryService = telemetryService;
	}

	@GetMapping("/devices/{deviceId}/latest")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public List<DeviceTelemetry> latest(@PathVariable Long deviceId) {
		return telemetryService.latest(deviceId);
	}
}

