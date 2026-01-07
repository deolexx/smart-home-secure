package com.smarthome.hub.repository;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceTelemetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeviceTelemetryRepository extends JpaRepository<DeviceTelemetry, Long> {
	List<DeviceTelemetry> findTop100ByDeviceOrderByTimestampDesc(Device device);
	List<DeviceTelemetry> findByDeviceAndTimestampBetweenOrderByTimestampDesc(Device device, Instant from, Instant to);
}

