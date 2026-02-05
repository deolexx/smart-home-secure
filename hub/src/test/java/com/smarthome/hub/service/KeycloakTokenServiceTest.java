package com.smarthome.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.hub.dto.KeycloakTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakTokenServiceTest {

	@Mock
	private RestTemplate restTemplate;

	private KeycloakTokenService.KeycloakClientProperties props;

	private KeycloakTokenService tokenService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		props = new KeycloakTokenService.KeycloakClientProperties();
		props.setServerUrl("http://keycloak");
		props.setRealm("realm");
		props.setClientId("client");
		props.setClientSecret("secret");
		tokenService = new KeycloakTokenService(restTemplate, props);
	}

	@Test
	void login_returnsTokenResponse() throws Exception {
		JsonNode responseBody = objectMapper.readTree("""
				{"access_token":"token","refresh_token":"refresh","token_type":"Bearer","expires_in":300,"refresh_expires_in":600,"scope":"openid"}
				""");
		when(restTemplate.exchange(
				ArgumentMatchers.eq("http://keycloak/realms/realm/protocol/openid-connect/token"),
				ArgumentMatchers.eq(HttpMethod.POST),
				ArgumentMatchers.any(HttpEntity.class),
				ArgumentMatchers.eq(JsonNode.class)
		)).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

		KeycloakTokenResponse result = tokenService.login("user", "pass");

		assertThat(result.getAccessToken()).isEqualTo("token");
		assertThat(result.getRefreshToken()).isEqualTo("refresh");
		assertThat(result.getTokenType()).isEqualTo("Bearer");
		assertThat(result.getExpiresIn()).isEqualTo(300);
		assertThat(result.getRefreshExpiresIn()).isEqualTo(600);
		assertThat(result.getScope()).isEqualTo("openid");
	}

	@Test
	void login_whenAccessTokenMissing_throws() throws Exception {
		JsonNode responseBody = objectMapper.readTree("{\"token_type\":\"Bearer\"}");
		when(restTemplate.exchange(
				ArgumentMatchers.eq("http://keycloak/realms/realm/protocol/openid-connect/token"),
				ArgumentMatchers.eq(HttpMethod.POST),
				ArgumentMatchers.any(HttpEntity.class),
				ArgumentMatchers.eq(JsonNode.class)
		)).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

		assertThatThrownBy(() -> tokenService.login("user", "pass"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Keycloak token missing in response");
	}
}
