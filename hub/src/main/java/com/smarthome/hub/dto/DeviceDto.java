package com.smarthome.hub.dto;

import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class DeviceDto {
	private UUID id;
	private String name;
	private DeviceType type;
	private DeviceStatus status;
	private String mqttClientId;
	private Instant updatedAt;
}

