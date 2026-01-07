package com.smarthome.hub.service;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.dto.CreateDeviceRequest;
import com.smarthome.hub.dto.UpdateDeviceRequest;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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

	@Transactional
	public Device createDevice(CreateDeviceRequest request) {
		Device device = deviceMapper.fromCreate(request);
		device.setStatus(DeviceStatus.OFFLINE);
		return deviceRepository.save(device);
	}

	@Transactional(readOnly = true)
	public Device getDevice(Long id) {
		return deviceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Device not found"));
	}

	@Transactional
	public Device updateDevice(Long id, UpdateDeviceRequest request) {
		Device device = getDevice(id);
		deviceMapper.update(device, request);
		return deviceRepository.save(device);
	}

	@Transactional
	public void deleteDevice(Long id) {
		deviceRepository.deleteById(id);
	}

	@Transactional
	public void markOnline(String clientId) {
		deviceRepository.findByMqttClientId(clientId).ifPresent(d -> {
			d.setStatus(DeviceStatus.ONLINE);
			d.setUpdatedAt(Instant.now());
			deviceRepository.save(d);
		});
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

