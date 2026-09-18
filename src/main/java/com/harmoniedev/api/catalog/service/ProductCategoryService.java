package com.harmoniedev.api.catalog.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.catalog.domain.dto.request.ProductCategoryRequest;
import com.harmoniedev.api.catalog.domain.dto.response.ProductCategoryResponse;
import com.harmoniedev.api.catalog.domain.model.ProductCategoryDocument;
import com.harmoniedev.api.catalog.repository.ProductCategoryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductCategoryService {
	private final ProductCategoryRepository repository;

	public ProductCategoryService(ProductCategoryRepository repository) {
		this.repository = repository;
	}

	public ProductCategoryResponse create(ProductCategoryRequest request, String actorId) {
		ProductCategoryDocument doc = ProductCategoryDocument.builder()
				.name(request.getName())
				.description(request.getDescription())
				.color(request.getColor())
				.enabled(request.isEnabled())
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<ProductCategoryResponse> list() {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public ProductCategoryResponse update(String id, ProductCategoryRequest request) {
		ProductCategoryDocument doc = findOrThrow(id);
		doc.setName(request.getName());
		doc.setDescription(request.getDescription());
		doc.setColor(request.getColor());
		doc.setEnabled(request.isEnabled());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		repository.delete(findOrThrow(id));
	}

	private ProductCategoryDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(d -> TenantScope.owns(d.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
	}

	private ProductCategoryResponse toResponse(ProductCategoryDocument doc) {
		return ProductCategoryResponse.builder()
				.id(doc.getId())
				.name(doc.getName())
				.description(doc.getDescription())
				.color(doc.getColor())
				.enabled(doc.isEnabled())
				.createdBy(doc.getCreatedBy())
				.build();
	}
}
