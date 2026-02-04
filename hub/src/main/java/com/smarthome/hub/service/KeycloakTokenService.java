package com.smarthome.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarthome.hub.dto.KeycloakTokenResponse;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class KeycloakTokenService {

	private final RestTemplate restTemplate;
	private final KeycloakClientProperties props;

	public KeycloakTokenService(RestTemplate restTemplate, KeycloakClientProperties props) {
		this.restTemplate = restTemplate;
		this.props = props;
	}

	public KeycloakTokenResponse login(String username, String password) {
		String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
				props.getServerUrl(), props.getRealm());

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "password");
		form.add("client_id", props.getClientId());
		form.add("username", username);
		form.add("password", password);
		if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
			form.add("client_secret", props.getClientSecret());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		ResponseEntity<JsonNode> response = restTemplate.exchange(
				tokenUrl,
				HttpMethod.POST,
				new HttpEntity<>(form, headers),
				JsonNode.class
		);

		if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
			throw new IllegalStateException("Failed to obtain Keycloak token");
		}

		JsonNode body = response.getBody();
		String accessToken = body.path("access_token").asText(null);
		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalStateException("Keycloak token missing in response");
		}

		return new KeycloakTokenResponse(
				accessToken,
				body.path("refresh_token").asText(null),
				body.path("token_type").asText(null),
				body.path("expires_in").asInt(0),
				body.path("refresh_expires_in").asInt(0),
				body.path("scope").asText(null)
		);
	}

	@Data
	@ConfigurationProperties(prefix = "keycloak")
	public static class KeycloakClientProperties {
		private String serverUrl = "http://localhost:8090";
		private String realm = "smarthome";
		private String clientId = "smart-home-hub";
		private String clientSecret;
	}
}
