package com.smarthome.hub.mapper;

import com.smarthome.hub.domain.Device;
import com.smarthome.hub.dto.DeviceDto;
import com.smarthome.hub.dto.CreateDeviceRequest;
import com.smarthome.hub.dto.UpdateDeviceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DeviceMapper {
	DeviceDto toDto(Device device);
	Device fromCreate(CreateDeviceRequest request);
	void update(@MappingTarget Device device, UpdateDeviceRequest request);
}

