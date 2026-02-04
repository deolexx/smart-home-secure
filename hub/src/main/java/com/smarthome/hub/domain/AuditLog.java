package com.smarthome.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(length = 64)
	private String userId;

	@Column(length = 128)
	private String username;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String roles;

	@Column(length = 16)
	private String method;

	@Column(length = 512)
	private String path;

	@Column(length = 1024)
	private String query;

	private Integer status;

	private Long durationMs;

	@Column(length = 64)
	private String clientIp;

	@Column(length = 256)
	private String userAgent;
}
