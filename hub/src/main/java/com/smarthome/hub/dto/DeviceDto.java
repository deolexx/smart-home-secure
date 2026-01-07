package com.smarthome.hub.dto;

import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import lombok.Data;

import java.time.Instant;

@Data
public class DeviceDto {
	private Long id;
	private String name;
	private DeviceType type;
	private DeviceStatus status;
	private String mqttClientId;
	private Instant updatedAt;
}

