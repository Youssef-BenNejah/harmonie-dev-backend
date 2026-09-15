package com.harmoniedev.api.notification.service;

import com.harmoniedev.api.notification.domain.dto.response.NotificationResponse;
import com.harmoniedev.api.notification.domain.model.NotificationDocument;
import com.harmoniedev.api.notification.repository.NotificationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
	private final NotificationRepository repository;

	public NotificationService(NotificationRepository repository) {
		this.repository = repository;
	}

	/** Fire-and-forget helper called by other services when something notification-worthy happens. */
	public void create(String type, String tenantAdminId, String title, String message, boolean forAdmin) {
		repository.save(NotificationDocument.builder()
				.type(type)
				.tenantAdminId(tenantAdminId)
				.title(title)
				.message(message)
				.forAdmin(forAdmin)
				.build());
	}

	public List<NotificationResponse> listForUser(String userId, boolean isAdmin) {
		List<NotificationDocument> docs = isAdmin
				? repository.findByForAdminTrueOrderByCreatedAtDesc()
				: repository.findByForAdminFalseAndTenantAdminIdOrderByCreatedAtDesc(userId);
		return docs.stream().map(this::toResponse).toList();
	}

	public void markRead(String id, String userId, boolean isAdmin) {
		NotificationDocument doc = findVisibleOrThrow(id, userId, isAdmin);
		doc.setRead(true);
		repository.save(doc);
	}

	public void markAllRead(String userId, boolean isAdmin) {
		List<NotificationDocument> docs = isAdmin
				? repository.findByForAdminTrueOrderByCreatedAtDesc()
				: repository.findByForAdminFalseAndTenantAdminIdOrderByCreatedAtDesc(userId);
		docs.forEach(d -> d.setRead(true));
		repository.saveAll(docs);
	}

	private NotificationDocument findVisibleOrThrow(String id, String userId, boolean isAdmin) {
		NotificationDocument doc = repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
		boolean visible = isAdmin ? doc.isForAdmin() : (!doc.isForAdmin() && userId.equals(doc.getTenantAdminId()));
		if (!visible) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
		}
		return doc;
	}

	private NotificationResponse toResponse(NotificationDocument doc) {
		return NotificationResponse.builder()
				.id(doc.getId())
				.type(doc.getType())
				.tenantAdminId(doc.getTenantAdminId())
				.title(doc.getTitle())
				.message(doc.getMessage())
				.createdAt(doc.getCreatedAt())
				.read(doc.isRead())
				.build();
	}
}
