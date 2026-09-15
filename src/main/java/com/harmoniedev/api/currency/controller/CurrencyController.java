package com.harmoniedev.api.currency.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.currency.domain.dto.request.CurrencyRequest;
import com.harmoniedev.api.currency.domain.dto.response.CurrencyResponse;
import com.harmoniedev.api.currency.service.CurrencyService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/currencies")
public class CurrencyController {
	private final CurrencyService service;

	public CurrencyController(CurrencyService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CurrencyResponse>> create(
			@Valid @RequestBody CurrencyRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Currency created", service.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<CurrencyResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Currencies", service.list()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CurrencyResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Currency", service.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<CurrencyResponse>> update(
			@PathVariable String id, @Valid @RequestBody CurrencyRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Currency updated", service.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Currency deleted", null));
	}
}
