package com.smarthome.hub.repository;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("resource")
class DeviceRepositoryIT {

	@Autowired
	private DeviceRepository deviceRepository;

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("smarthome")
			.withUsername("smarthome")
			.withPassword("smarthome");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
	}

	@Test
	void findAllByOwnerOrUnclaimed_returnsOwnedAndUnclaimedDevices() {
		Device owned = deviceRepository.save(Device.builder()
				.name("Owned")
				.type(DeviceType.LIGHT)
				.status(DeviceStatus.OFFLINE)
				.ownerKeycloakId("user-1")
				.build());
		deviceRepository.save(Device.builder()
				.name("Unclaimed")
				.type(DeviceType.SENSOR)
				.status(DeviceStatus.ONLINE)
				.build());
		deviceRepository.save(Device.builder()
				.name("Other")
				.type(DeviceType.THERMOSTAT)
				.status(DeviceStatus.ONLINE)
				.ownerKeycloakId("user-2")
				.build());

		List<Device> result = deviceRepository.findAllByOwnerKeycloakIdOrOwnerKeycloakIdIsNull("user-1");

		assertThat(result).extracting(Device::getName).containsExactlyInAnyOrder("Owned", "Unclaimed");
		assertThat(result).extracting(Device::getId).contains(owned.getId());
	}
}
