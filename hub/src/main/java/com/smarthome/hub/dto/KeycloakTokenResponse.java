package com.smarthome.hub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeycloakTokenResponse {
	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private int expiresIn;
	private int refreshExpiresIn;
	private String scope;
}
