package com.harmoniedev.api.plan.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.plan.domain.dto.request.PlanRequest;
import com.harmoniedev.api.plan.domain.dto.response.PlanResponse;
import com.harmoniedev.api.plan.service.PlanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plans are public reference data (🌐 in the doc); write endpoints are Super Admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {
	private final PlanService service;

	public PlanController(PlanService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PlanResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Plans", service.list()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PlanResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Plan", service.get(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody PlanRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Plan created", service.create(request)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<PlanResponse>> update(
			@PathVariable String id, @Valid @RequestBody PlanRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Plan updated", service.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Plan deleted", null));
	}
}
