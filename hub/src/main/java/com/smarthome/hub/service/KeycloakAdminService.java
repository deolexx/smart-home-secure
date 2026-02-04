package com.smarthome.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarthome.hub.dto.RegisterRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakAdminService {

	private final RestTemplate restTemplate;
	private final KeycloakAdminProperties props;

	public KeycloakAdminService(RestTemplate restTemplate, KeycloakAdminProperties props) {
		this.restTemplate = restTemplate;
		this.props = props;
	}

	public void registerUser(RegisterRequest request, boolean admin) {
		String token = getAdminAccessToken();
		String userId = createUser(token, request);
		assignRealmRole(token, userId, admin ? "ADMIN" : "USER");
	}

	private String getAdminAccessToken() {
		String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
				props.getServerUrl(), props.getAdminRealm());

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "password");
		form.add("client_id", props.getAdminClientId());
		form.add("username", props.getAdminUsername());
		form.add("password", props.getAdminPassword());
		if (props.getAdminClientSecret() != null && !props.getAdminClientSecret().isBlank()) {
			form.add("client_secret", props.getAdminClientSecret());
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
			throw new IllegalStateException("Failed to obtain Keycloak admin token");
		}
		JsonNode tokenNode = response.getBody().get("access_token");
		if (tokenNode == null || tokenNode.asText().isBlank()) {
			throw new IllegalStateException("Keycloak admin token missing in response");
		}
		return tokenNode.asText();
	}

	private String createUser(String token, RegisterRequest request) {
		String createUrl = String.format("%s/admin/realms/%s/users", props.getServerUrl(), props.getRealm());

		Map<String, Object> payload = Map.of(
				"username", request.getUsername(),
				"email", request.getEmail(),
				"enabled", true,
				"emailVerified", true,
				"requiredActions", List.of(),
				"credentials", List.of(Map.of(
						"type", "password",
						"value", request.getPassword(),
						"temporary", false
				))
		);

		try {
			ResponseEntity<Void> response = restTemplate.exchange(
					createUrl,
					HttpMethod.POST,
					new HttpEntity<>(payload, bearerHeaders(token)),
					Void.class
			);
			if (response.getStatusCode() != HttpStatus.CREATED) {
				throw new IllegalStateException("Keycloak user creation failed");
			}
			URI location = response.getHeaders().getLocation();
			if (location == null) {
				throw new IllegalStateException("Keycloak did not return user location");
			}
			String path = location.getPath();
			return path.substring(path.lastIndexOf('/') + 1);
		} catch (HttpClientErrorException.Conflict ex) {
			throw new IllegalArgumentException("Username or email already exists");
		} catch (HttpClientErrorException ex) {
			log.error("Keycloak user creation failed: {}", ex.getResponseBodyAsString());
			throw new IllegalStateException("Keycloak user creation failed");
		}
	}

	private void assignRealmRole(String token, String userId, String roleName) {
		String roleUrl = String.format("%s/admin/realms/%s/roles/%s",
				props.getServerUrl(), props.getRealm(), roleName);
		ResponseEntity<JsonNode> roleResponse = restTemplate.exchange(
				roleUrl,
				HttpMethod.GET,
				new HttpEntity<>(bearerHeaders(token)),
				JsonNode.class
		);
		if (!roleResponse.getStatusCode().is2xxSuccessful() || roleResponse.getBody() == null) {
			throw new IllegalStateException("Failed to fetch role " + roleName);
		}

		JsonNode role = roleResponse.getBody();
		Map<String, Object> roleRep = Map.of(
				"id", role.get("id").asText(),
				"name", role.get("name").asText()
		);

		String mappingUrl = String.format("%s/admin/realms/%s/users/%s/role-mappings/realm",
				props.getServerUrl(), props.getRealm(), userId);
		ResponseEntity<Void> mapResponse = restTemplate.exchange(
				mappingUrl,
				HttpMethod.POST,
				new HttpEntity<>(List.of(roleRep), bearerHeaders(token)),
				Void.class
		);
		if (!mapResponse.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException("Failed to assign role " + roleName);
		}
	}

	private HttpHeaders bearerHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	@Data
	@ConfigurationProperties(prefix = "keycloak.admin")
	public static class KeycloakAdminProperties {
		private String serverUrl = "http://localhost:8090";
		private String realm = "smarthome";
		private String adminRealm = "master";
		private String adminClientId = "admin-cli";
		private String adminClientSecret;
		private String adminUsername = "admin";
		private String adminPassword = "admin123!";
	}
}
