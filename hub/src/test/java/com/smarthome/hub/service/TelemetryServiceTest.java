package com.smarthome.hub.service;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceTelemetry;
import com.smarthome.hub.domain.DeviceType;
import com.smarthome.hub.repository.DeviceRepository;
import com.smarthome.hub.repository.DeviceTelemetryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

	@Mock
	private DeviceRepository deviceRepository;

	@Mock
	private DeviceTelemetryRepository telemetryRepository;

	@InjectMocks
	private TelemetryService telemetryService;

	@Test
	void ingestTelemetry_parsesStructuredFields() {
		Device device = Device.builder().id(UUID.randomUUID()).name("sensor").type(DeviceType.SENSOR).build();
		when(deviceRepository.findByMqttClientId("client-1")).thenReturn(Optional.of(device));

		telemetryService.ingestTelemetry("client-1", "{\"temperature\":21.5,\"humidity\":40.0,\"status\":\"OK\"}");

		ArgumentCaptor<DeviceTelemetry> captor = ArgumentCaptor.forClass(DeviceTelemetry.class);
		verify(telemetryRepository).save(captor.capture());
		DeviceTelemetry saved = captor.getValue();
		assertThat(saved.getDevice()).isEqualTo(device);
		assertThat(saved.getTemperature()).isEqualTo(21.5);
		assertThat(saved.getHumidity()).isEqualTo(40.0);
		assertThat(saved.getStatus()).isEqualTo("OK");
		assertThat(saved.getRawJson()).contains("\"temperature\":21.5");
	}

	@Test
	void ingestTelemetry_withInvalidJson_storesRawPayload() {
		Device device = Device.builder().id(UUID.randomUUID()).name("sensor").type(DeviceType.SENSOR).build();
		when(deviceRepository.findByMqttClientId("client-2")).thenReturn(Optional.of(device));

		telemetryService.ingestTelemetry("client-2", "{not-json");

		ArgumentCaptor<DeviceTelemetry> captor = ArgumentCaptor.forClass(DeviceTelemetry.class);
		verify(telemetryRepository).save(captor.capture());
		DeviceTelemetry saved = captor.getValue();
		assertThat(saved.getTemperature()).isNull();
		assertThat(saved.getHumidity()).isNull();
		assertThat(saved.getStatus()).isNull();
		assertThat(saved.getRawJson()).isEqualTo("{not-json");
	}

	@Test
	void latest_returnsLatestTelemetryForDevice() {
		UUID deviceId = UUID.randomUUID();
		Device device = Device.builder().id(deviceId).name("sensor").type(DeviceType.SENSOR).build();
		List<DeviceTelemetry> telemetry = List.of(DeviceTelemetry.builder().device(device).build());
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
		when(telemetryRepository.findTop100ByDeviceOrderByTimestampDesc(device)).thenReturn(telemetry);

		List<DeviceTelemetry> result = telemetryService.latest(deviceId);

		assertThat(result).isEqualTo(telemetry);
		verify(telemetryRepository).findTop100ByDeviceOrderByTimestampDesc(device);
		verify(telemetryRepository, never()).save(any());
	}
}
