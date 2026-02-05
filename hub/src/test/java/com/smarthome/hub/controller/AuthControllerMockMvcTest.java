package com.smarthome.hub.controller;

import com.smarthome.hub.TestSecurityConfig;
import com.smarthome.hub.dto.KeycloakTokenResponse;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.service.DeviceService;
import com.smarthome.hub.service.AuditLogService;
import com.smarthome.hub.service.KeycloakAdminService;
import com.smarthome.hub.service.KeycloakTokenService;
import com.smarthome.hub.mqtt.MqttGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AuthControllerMockMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private KeycloakAdminService keycloakAdminService;

	@MockBean
	private KeycloakTokenService keycloakTokenService;

	@MockBean
	private DeviceService deviceService;

	@MockBean
	private DeviceMapper deviceMapper;

	@MockBean
	private AuditLogService auditLogService;

	@MockBean
	private MqttGateway mqttGateway;

	@Test
	void register_returnsCreatedResponse() throws Exception {
		doNothing().when(keycloakAdminService).registerUser(any(), anyBoolean());

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"alice","email":"alice@example.com","password":"secret"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	void token_returnsTokenResponse() throws Exception {
		KeycloakTokenResponse response = new KeycloakTokenResponse("token", "refresh", "Bearer", 300, 600, "openid");
		when(keycloakTokenService.login("alice", "secret")).thenReturn(response);

		mockMvc.perform(post("/api/auth/token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"alice","password":"secret"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh"));
	}

	@Test
	void authInfo_returnsKeycloakEndpoints() throws Exception {
		mockMvc.perform(get("/api/auth/info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenEndpoint").exists())
				.andExpect(jsonPath("$.authorizationEndpoint").exists());
	}
}
