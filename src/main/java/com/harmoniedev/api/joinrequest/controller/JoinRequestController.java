package com.harmoniedev.api.joinrequest.controller;

import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.joinrequest.domain.dto.request.ConvertJoinRequestRequest;
import com.harmoniedev.api.joinrequest.domain.dto.request.CreateJoinRequestRequest;
import com.harmoniedev.api.joinrequest.domain.dto.response.JoinRequestResponse;
import com.harmoniedev.api.joinrequest.service.JoinRequestService;
import com.harmoniedev.api.security.AuthenticatedUser;
import com.harmoniedev.api.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/join-requests")
public class JoinRequestController {
	private final JoinRequestService joinRequestService;
	private final ClientIpResolver clientIpResolver;

	public JoinRequestController(JoinRequestService joinRequestService, ClientIpResolver clientIpResolver) {
		this.clientIpResolver = clientIpResolver;
		this.joinRequestService = joinRequestService;
	}

	/** Public — submitted from the marketing landing page's "Rejoindre" form. */
	@PostMapping
	public ResponseEntity<ApiResponse<JoinRequestResponse>> submit(
			@Valid @RequestBody CreateJoinRequestRequest request, HttpServletRequest httpRequest) {
		JoinRequestResponse response = joinRequestService.submit(
				request, resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Join request submitted", response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<JoinRequestResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Join requests", joinRequestService.list()));
	}

	@PatchMapping("/{id}/contact")
	public ResponseEntity<ApiResponse<JoinRequestResponse>> contact(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Marked contacted", joinRequestService.markContacted(id)));
	}

	@PatchMapping("/{id}/reject")
	public ResponseEntity<ApiResponse<JoinRequestResponse>> reject(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Rejected", joinRequestService.reject(id)));
	}

	@PostMapping("/{id}/convert")
	public ResponseEntity<ApiResponse<UserResponse>> convert(
			@PathVariable String id,
			@RequestBody(required = false) ConvertJoinRequestRequest request,
			Authentication authentication,
			HttpServletRequest httpRequest) {
		AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
		Integer trialDays = request != null ? request.getTrialDays() : null;
		String planId = request != null ? request.getPlanId() : null;
		UserResponse user = joinRequestService.convert(
				id, trialDays, planId, principal.getId(), resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
		return ResponseEntity.ok(ApiResponse.success("Converted — credentials sent by e-mail", user));
	}

	private String resolveClientIp(HttpServletRequest request) {
		return clientIpResolver.resolve(request);
	}
}
