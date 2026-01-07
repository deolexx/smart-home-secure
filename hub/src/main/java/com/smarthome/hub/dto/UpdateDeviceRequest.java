package com.smarthome.hub.dto;

import com.smarthome.hub.domain.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDeviceRequest {
	@NotBlank
	private String name;
	@NotNull
	private DeviceType type;
}

