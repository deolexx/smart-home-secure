package com.smarthome.hub.service;

import com.smarthome.hub.domain.AuditLog;
import com.smarthome.hub.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;

	public AuditLogService(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(AuditLog log) {
		auditLogRepository.save(log);
	}
}
