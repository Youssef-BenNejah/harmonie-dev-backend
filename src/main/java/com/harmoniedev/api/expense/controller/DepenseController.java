package com.harmoniedev.api.expense.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.common.paging.Paging;
import com.harmoniedev.api.expense.domain.dto.request.DepenseRequest;
import com.harmoniedev.api.expense.domain.dto.response.DepenseResponse;
import com.harmoniedev.api.expense.service.DepenseService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/depenses")
public class DepenseController {
	private final DepenseService service;

	public DepenseController(DepenseService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<DepenseResponse>> create(
			@Valid @RequestBody DepenseRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Depense created", service.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<DepenseResponse>>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return Paging.respond("Depenses", service.listPage(Paging.of(page, size)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<DepenseResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Depense", service.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<DepenseResponse>> update(
			@PathVariable String id, @Valid @RequestBody DepenseRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Depense updated", service.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Depense deleted", null));
	}
}
