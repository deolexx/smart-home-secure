package com.smarthome.hub.service;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.domain.DeviceStatus;
import com.smarthome.hub.domain.DeviceType;
import com.smarthome.hub.dto.CreateDeviceRequest;
import com.smarthome.hub.mapper.DeviceMapper;
import com.smarthome.hub.repository.DeviceRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

	@Mock
	private DeviceRepository deviceRepository;

	@Mock
	private DeviceMapper deviceMapper;

	@InjectMocks
	private DeviceService deviceService;

	@Test
	void listDevicesForUser_whenAdmin_returnsAllDevices() {
		List<Device> devices = List.of(Device.builder().name("a").type(DeviceType.SENSOR).build());
		when(deviceRepository.findAll()).thenReturn(devices);

		List<Device> result = deviceService.listDevicesForUser("user", true);

		assertThat(result).isEqualTo(devices);
		verify(deviceRepository).findAll();
		verify(deviceRepository, never()).findAllByOwnerKeycloakIdOrOwnerKeycloakIdIsNull(any());
	}

	@Test
	void listDevicesForUser_whenNotAdmin_filtersByOwner() {
		List<Device> devices = List.of(Device.builder().name("b").type(DeviceType.LIGHT).build());
		when(deviceRepository.findAllByOwnerKeycloakIdOrOwnerKeycloakIdIsNull("user")).thenReturn(devices);

		List<Device> result = deviceService.listDevicesForUser("user", false);

		assertThat(result).isEqualTo(devices);
		verify(deviceRepository).findAllByOwnerKeycloakIdOrOwnerKeycloakIdIsNull("user");
		verify(deviceRepository, never()).findAll();
	}

	@Test
	void getDeviceForUser_whenNotOwner_throws() {
		UUID deviceId = UUID.randomUUID();
		Device device = Device.builder()
				.id(deviceId)
				.name("device")
				.type(DeviceType.SENSOR)
				.ownerKeycloakId("other-user")
				.build();
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

		assertThatThrownBy(() -> deviceService.getDeviceForUser(deviceId, "user", false))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Device not accessible");
	}

	@Test
	void createDevice_setsOfflineStatus() {
		CreateDeviceRequest request = new CreateDeviceRequest();
		Device mapped = Device.builder().name("new-device").type(DeviceType.SENSOR).build();
		when(deviceMapper.fromCreate(request)).thenReturn(mapped);
		when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Device created = deviceService.createDevice(request);

		ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
		verify(deviceRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(DeviceStatus.OFFLINE);
		assertThat(created.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
	}

	@Test
	void claimDevice_whenAlreadyClaimed_throws() {
		UUID deviceId = UUID.randomUUID();
		Device device = Device.builder()
				.id(deviceId)
				.name("claimed")
				.type(DeviceType.SENSOR)
				.ownerKeycloakId("owner")
				.build();
		when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

		assertThatThrownBy(() -> deviceService.claimDevice(deviceId, "user"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Device already claimed");
	}
}
