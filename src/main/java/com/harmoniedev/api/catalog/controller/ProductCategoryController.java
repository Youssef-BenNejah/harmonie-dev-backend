package com.harmoniedev.api.catalog.controller;

import com.harmoniedev.api.catalog.domain.dto.request.ProductCategoryRequest;
import com.harmoniedev.api.catalog.domain.dto.response.ProductCategoryResponse;
import com.harmoniedev.api.catalog.service.ProductCategoryService;
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
@RequestMapping("/api/v1/service-categories")
public class ProductCategoryController {
	private final ProductCategoryService service;

	public ProductCategoryController(ProductCategoryService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ProductCategoryResponse>> create(
			@Valid @RequestBody ProductCategoryRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Category created", service.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ProductCategoryResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Categories", service.list()));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductCategoryResponse>> update(
			@PathVariable String id, @Valid @RequestBody ProductCategoryRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Category updated", service.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
	}
}
