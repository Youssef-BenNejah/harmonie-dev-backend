package com.harmoniedev.api.notification.repository;

import com.harmoniedev.api.notification.domain.model.NotificationDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
	List<NotificationDocument> findByForAdminTrueOrderByCreatedAtDesc();

	List<NotificationDocument> findByForAdminFalseAndTenantAdminIdOrderByCreatedAtDesc(String tenantAdminId);
}
