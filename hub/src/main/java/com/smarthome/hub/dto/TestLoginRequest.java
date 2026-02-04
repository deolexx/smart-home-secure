package com.smarthome.hub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestLoginRequest {
	@NotBlank
	private String username;
	
	@NotBlank
	private String password;
}
