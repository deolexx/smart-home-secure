package com.smarthome.hub.repository;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
	Optional<Device> findByMqttClientId(String clientId);
	List<Device> findAllByStatus(DeviceStatus status);
}

