package com.harmoniedev.api.catalog.controller;

import com.harmoniedev.api.catalog.domain.dto.request.ProductRequest;
import com.harmoniedev.api.catalog.domain.dto.response.ProductResponse;
import com.harmoniedev.api.catalog.service.ProductService;
import com.harmoniedev.api.common.dto.ApiResponse;
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
@RequestMapping("/api/v1/services")
public class ProductController {
	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ProductResponse>> create(
			@Valid @RequestBody ProductRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Service created", service.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ProductResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Services", service.list()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Service", service.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> update(
			@PathVariable String id, @Valid @RequestBody ProductRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Service updated", service.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Service deleted", null));
	}
}
