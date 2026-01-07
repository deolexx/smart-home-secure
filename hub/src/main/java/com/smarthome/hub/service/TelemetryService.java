package com.smarthome.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceTelemetry;
import com.smarthome.hub.repository.DeviceRepository;
import com.smarthome.hub.repository.DeviceTelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class TelemetryService {

	private final DeviceRepository deviceRepository;
	private final DeviceTelemetryRepository telemetryRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public TelemetryService(DeviceRepository deviceRepository, DeviceTelemetryRepository telemetryRepository) {
		this.deviceRepository = deviceRepository;
		this.telemetryRepository = telemetryRepository;
	}

	@Transactional
	public void ingestTelemetry(String clientId, String payloadJson) {
		deviceRepository.findByMqttClientId(clientId).ifPresent(device -> {
			DeviceTelemetry telemetry = parseTelemetry(device, payloadJson);
			telemetryRepository.save(telemetry);
		});
	}

	@Transactional(readOnly = true)
	public List<DeviceTelemetry> latest(Long deviceId) {
		Device device = deviceRepository.findById(deviceId).orElseThrow();
		return telemetryRepository.findTop100ByDeviceOrderByTimestampDesc(device);
	}

	private DeviceTelemetry parseTelemetry(Device device, String json) {
		try {
			JsonNode node = objectMapper.readTree(json);
			Double temperature = node.has("temperature") ? node.get("temperature").asDouble() : null;
			Double humidity = node.has("humidity") ? node.get("humidity").asDouble() : null;
			String status = node.has("status") ? node.get("status").asText() : null;
			return DeviceTelemetry.builder()
					.device(device)
					.temperature(temperature)
					.humidity(humidity)
					.status(status)
					.rawJson(json)
					.build();
		} catch (IOException e) {
			return DeviceTelemetry.builder()
					.device(device)
					.rawJson(json)
					.build();
		}
	}
}

