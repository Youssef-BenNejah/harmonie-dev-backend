package com.harmoniedev.api.audit.repository;

import com.harmoniedev.api.audit.domain.model.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String> {
}