package com.harmoniedev.api.plan.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.plan.domain.dto.response.PlanUsageResponse;
import com.harmoniedev.api.plan.service.PlanUsageService;
import com.harmoniedev.api.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the current tenant see how much of their plan's quotas they've consumed. */
@RestController
@RequestMapping("/api/v1/plan-usage")
public class PlanUsageController {
	private final PlanUsageService service;

	public PlanUsageController(PlanUsageService service) {
		this.service = service;
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<PlanUsageResponse>> me(Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Plan usage", service.getUsage(actorId)));
	}
}
