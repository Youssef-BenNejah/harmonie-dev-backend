package com.harmoniedev.api.tax.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.security.AuthenticatedUser;
import com.harmoniedev.api.tax.domain.dto.request.TaxRequest;
import com.harmoniedev.api.tax.domain.dto.response.TaxResponse;
import com.harmoniedev.api.tax.service.TaxService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/taxes")
public class TaxController {
	private final TaxService taxService;

	public TaxController(TaxService taxService) {
		this.taxService = taxService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<TaxResponse>> create(
			@Valid @RequestBody TaxRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Tax created", taxService.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TaxResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Taxes", taxService.list()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<TaxResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Tax", taxService.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<TaxResponse>> update(
			@PathVariable String id, @Valid @RequestBody TaxRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Tax updated", taxService.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		taxService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Tax deleted", null));
	}

	@PostMapping("/{id}/set-default")
	public ResponseEntity<ApiResponse<TaxResponse>> setDefault(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Default tax set", taxService.setDefault(id)));
	}
}
