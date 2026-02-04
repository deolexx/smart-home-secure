package com.smarthome.hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class TemperatureUnitCommandRequest {
	@NotBlank
	@Pattern(regexp = "(?i)C|F", message = "unit must be C or F")
	@Schema(description = "Temperature unit", example = "C", allowableValues = {"C", "F"})
	private String unit;
}
