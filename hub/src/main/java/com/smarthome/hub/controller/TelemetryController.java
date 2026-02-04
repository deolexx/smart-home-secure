package com.smarthome.hub.controller;

import com.smarthome.hub.domain.DeviceTelemetry;
import com.smarthome.hub.service.DeviceService;
import com.smarthome.hub.service.TelemetryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

	private final TelemetryService telemetryService;
	private final DeviceService deviceService;

	public TelemetryController(TelemetryService telemetryService, DeviceService deviceService) {
		this.telemetryService = telemetryService;
		this.deviceService = deviceService;
	}

	@GetMapping("/devices/{deviceId}/latest")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public List<DeviceTelemetry> latest(@PathVariable UUID deviceId) {
		Authentication auth = currentAuth();
		deviceService.getDeviceForUser(deviceId, currentUserId(auth), isAdmin(auth));
		return telemetryService.latest(deviceId);
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

