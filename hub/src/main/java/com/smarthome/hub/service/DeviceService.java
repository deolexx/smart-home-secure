package com.smarthome.hub.service;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import com.smarthome.hub.dto.CreateDeviceRequest;
import com.smarthome.hub.dto.UpdateDeviceRequest;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

	private final DeviceRepository deviceRepository;
	private final DeviceMapper deviceMapper;

	public DeviceService(DeviceRepository deviceRepository, DeviceMapper deviceMapper) {
		this.deviceRepository = deviceRepository;
		this.deviceMapper = deviceMapper;
	}

	@Transactional(readOnly = true)
	public List<Device> listDevices() {
		return deviceRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Device> listDevicesForUser(String keycloakUserId, boolean isAdmin) {
		if (isAdmin) {
			return deviceRepository.findAll();
		}
		return deviceRepository.findAllByOwnerKeycloakIdOrOwnerKeycloakIdIsNull(keycloakUserId);
	}

	@Transactional
	public Device createDevice(CreateDeviceRequest request) {
		Device device = deviceMapper.fromCreate(request);
		device.setStatus(DeviceStatus.OFFLINE);
		return deviceRepository.save(device);
	}

	@Transactional(readOnly = true)
	public Device getDevice(UUID id) {
		return deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Device not found"));
	}

	@Transactional(readOnly = true)
	public Device getDeviceForUser(UUID id, String keycloakUserId, boolean isAdmin) {
		Device device = getDevice(id);
		if (isAdmin) {
			return device;
		}
		String owner = device.getOwnerKeycloakId();
		if (owner == null || owner.equals(keycloakUserId)) {
			return device;
		}
		throw new IllegalArgumentException("Device not accessible");
	}

	@Transactional(readOnly = true)
	public Device getDeviceByClientId(String clientId) {
		return deviceRepository.findByMqttClientId(clientId)
				.orElseThrow(() -> new IllegalArgumentException("Device not found"));
	}

	@Transactional
	public Device updateDevice(UUID id, UpdateDeviceRequest request) {
		Device device = getDevice(id);
		deviceMapper.update(device, request);
		return deviceRepository.save(device);
	}

	@Transactional
	public void deleteDevice(UUID id) {
		deviceRepository.deleteById(id);
	}

	@Transactional
	public void markOnline(String clientId) {
		deviceRepository.findByMqttClientId(clientId).ifPresentOrElse(d -> {
			d.setStatus(DeviceStatus.ONLINE);
			d.setUpdatedAt(Instant.now());
			deviceRepository.save(d);
		}, () -> {
			Device device = Device.builder()
					.name("Auto-" + clientId)
					.type(DeviceType.SENSOR)
					.status(DeviceStatus.ONLINE)
					.mqttClientId(clientId)
					.build();
			deviceRepository.save(device);
		});
	}

	@Transactional
	public Device claimDevice(UUID id, String keycloakUserId) {
		Device device = getDevice(id);
		if (device.getOwnerKeycloakId() != null) {
			throw new IllegalStateException("Device already claimed");
		}
		device.setOwnerKeycloakId(keycloakUserId);
		return deviceRepository.save(device);
	}

	@Transactional
	public void markOffline(String clientId) {
		deviceRepository.findByMqttClientId(clientId).ifPresent(d -> {
			d.setStatus(DeviceStatus.OFFLINE);
			d.setUpdatedAt(Instant.now());
			deviceRepository.save(d);
		});
	}
}

