package com.smarthome.hub.controller;

import com.smarthome.hub.TestSecurityConfig;
import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import com.smarthome.hub.dto.DeviceDto;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.mqtt.MqttGateway;
import com.smarthome.hub.service.AuditLogService;
import com.smarthome.hub.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DeviceControllerMockMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DeviceService deviceService;

	@MockBean
	private DeviceMapper deviceMapper;

	@MockBean
	private MqttGateway mqttGateway;

	@MockBean
	private AuditLogService auditLogService;

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void list_returnsDevicesForAdmin() throws Exception {
		Device device = Device.builder()
				.id(UUID.randomUUID())
				.name("Thermostat")
				.type(DeviceType.THERMOSTAT)
				.status(DeviceStatus.ONLINE)
				.build();
		DeviceDto dto = new DeviceDto();
		dto.setId(device.getId());
		dto.setName(device.getName());
		dto.setType(device.getType());
		dto.setStatus(device.getStatus());

		when(deviceService.listDevicesForUser(eq("admin"), eq(true))).thenReturn(List.of(device));
		when(deviceMapper.toDto(device)).thenReturn(dto);

		mockMvc.perform(get("/api/devices"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Thermostat"))
				.andExpect(jsonPath("$[0].status").value("ONLINE"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void create_returnsCreatedDevice() throws Exception {
		UUID deviceId = UUID.randomUUID();
		Device created = Device.builder()
				.id(deviceId)
				.name("New Device")
				.type(DeviceType.SENSOR)
				.status(DeviceStatus.OFFLINE)
				.build();
		DeviceDto dto = new DeviceDto();
		dto.setId(deviceId);
		dto.setName("New Device");
		dto.setType(DeviceType.SENSOR);
		dto.setStatus(DeviceStatus.OFFLINE);

		when(deviceService.createDevice(any())).thenReturn(created);
		when(deviceMapper.toDto(created)).thenReturn(dto);

		mockMvc.perform(post("/api/devices")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"New Device","type":"SENSOR"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/devices/" + deviceId))
				.andExpect(jsonPath("$.name").value("New Device"))
				.andExpect(jsonPath("$.status").value("OFFLINE"));
	}
}
