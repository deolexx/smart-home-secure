package com.smarthome.hub.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "device_telemetry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTelemetry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "device_id", nullable = false)
	private Device device;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant timestamp;

	// Example structured fields
	private Double temperature;
	private Double humidity;
	private String status;

	// Raw JSON payload for extensibility
	@Lob
	@Column(columnDefinition = "TEXT")
	private String rawJson;
}

