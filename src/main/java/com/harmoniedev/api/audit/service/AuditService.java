package com.harmoniedev.api.audit.service;

import com.harmoniedev.api.audit.domain.enums.AuditAction;
import com.harmoniedev.api.audit.domain.model.AuditLogDocument;
import com.harmoniedev.api.audit.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
	private final AuditLogRepository repository;

	public AuditService(AuditLogRepository repository) {
		this.repository = repository;
	}

	@Async("auditTaskExecutor")
	public void log(String userId, AuditAction action, String ipAddress, String userAgent) {
		AuditLogDocument doc = AuditLogDocument.builder()
				.userId(userId)
				.action(action)
				.ipAddress(ipAddress)
				.userAgent(userAgent)
				.build();
		repository.save(doc);
	}
}
