package com.harmoniedev.api.notification.controller;

import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.notification.domain.dto.response.NotificationResponse;
import com.harmoniedev.api.notification.service.NotificationService;
import com.harmoniedev.api.security.AuthenticatedUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
	private final NotificationService service;

	public NotificationController(NotificationService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(Authentication authentication) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		return ResponseEntity.ok(ApiResponse.success(
				"Notifications", service.listForUser(principal.getId(), principal.getRole() == Role.ADMIN)));
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable String id, Authentication authentication) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		service.markRead(id, principal.getId(), principal.getRole() == Role.ADMIN);
		return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
	}

	@PatchMapping("/read-all")
	public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication authentication) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		service.markAllRead(principal.getId(), principal.getRole() == Role.ADMIN);
		return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
	}
}
