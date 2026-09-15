package com.harmoniedev.api.catalog.service;

import com.harmoniedev.api.catalog.domain.dto.request.ProductRequest;
import com.harmoniedev.api.catalog.domain.dto.response.ProductResponse;
import com.harmoniedev.api.catalog.domain.model.ProductDocument;
import com.harmoniedev.api.catalog.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
	private final ProductRepository repository;

	public ProductService(ProductRepository repository) {
		this.repository = repository;
	}

	public ProductResponse create(ProductRequest request, String actorId) {
		ProductDocument doc = ProductDocument.builder()
				.name(request.getName())
				.currency(request.getCurrency())
				.price(request.getPrice())
				.description(request.getDescription())
				.reference(request.getReference())
				.categoryId(request.getCategoryId())
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<ProductResponse> list() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	public ProductResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public ProductResponse update(String id, ProductRequest request) {
		ProductDocument doc = findOrThrow(id);
		doc.setName(request.getName());
		doc.setCurrency(request.getCurrency());
		doc.setPrice(request.getPrice());
		doc.setDescription(request.getDescription());
		doc.setReference(request.getReference());
		doc.setCategoryId(request.getCategoryId());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
		}
		repository.deleteById(id);
	}

	private ProductDocument findOrThrow(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
	}

	private ProductResponse toResponse(ProductDocument doc) {
		return ProductResponse.builder()
				.id(doc.getId())
				.name(doc.getName())
				.currency(doc.getCurrency())
				.price(doc.getPrice())
				.description(doc.getDescription())
				.reference(doc.getReference())
				.categoryId(doc.getCategoryId())
				.createdBy(doc.getCreatedBy())
				.created(doc.getCreated())
				.build();
	}
}
