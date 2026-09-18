package com.harmoniedev.api.expense.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.expense.domain.dto.request.DepenseRequest;
import com.harmoniedev.api.expense.domain.dto.response.DepenseResponse;
import com.harmoniedev.api.expense.domain.model.DepenseDocument;
import com.harmoniedev.api.expense.repository.DepenseRepository;
import com.harmoniedev.api.plan.service.PlanUsageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepenseService {
	private final DepenseRepository repository;
	private final PlanUsageService planUsageService;

	public DepenseService(DepenseRepository repository, PlanUsageService planUsageService) {
		this.repository = repository;
		this.planUsageService = planUsageService;
	}

	public DepenseResponse create(DepenseRequest request, String actorId) {
		planUsageService.assertExpensesEnabled(actorId);
		DepenseDocument doc = DepenseDocument.builder()
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

	public List<DepenseResponse> list() {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public DepenseResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public DepenseResponse update(String id, DepenseRequest request) {
		DepenseDocument doc = findOrThrow(id);
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
		repository.delete(findOrThrow(id));
	}

	private DepenseDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(d -> TenantScope.owns(d.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Depense not found"));
	}

	private DepenseResponse toResponse(DepenseDocument doc) {
		return DepenseResponse.builder()
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
