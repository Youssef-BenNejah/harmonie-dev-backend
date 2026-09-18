package com.harmoniedev.api.expense.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.expense.domain.dto.request.DepenseCategoryRequest;
import com.harmoniedev.api.expense.domain.dto.response.DepenseCategoryResponse;
import com.harmoniedev.api.expense.domain.model.DepenseCategoryDocument;
import com.harmoniedev.api.expense.repository.DepenseCategoryRepository;
import com.harmoniedev.api.plan.service.PlanUsageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepenseCategoryService {
	private final DepenseCategoryRepository repository;
	private final PlanUsageService planUsageService;

	public DepenseCategoryService(DepenseCategoryRepository repository, PlanUsageService planUsageService) {
		this.repository = repository;
		this.planUsageService = planUsageService;
	}

	public DepenseCategoryResponse create(DepenseCategoryRequest request, String actorId) {
		planUsageService.assertExpensesEnabled(actorId);
		DepenseCategoryDocument doc = DepenseCategoryDocument.builder()
				.name(request.getName())
				.color(request.getColor())
				.enabled(request.isEnabled())
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<DepenseCategoryResponse> list() {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public DepenseCategoryResponse update(String id, DepenseCategoryRequest request) {
		DepenseCategoryDocument doc = findOrThrow(id);
		doc.setName(request.getName());
		doc.setColor(request.getColor());
		doc.setEnabled(request.isEnabled());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		repository.delete(findOrThrow(id));
	}

	private DepenseCategoryDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(d -> TenantScope.owns(d.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
	}

	private DepenseCategoryResponse toResponse(DepenseCategoryDocument doc) {
		return DepenseCategoryResponse.builder()
				.id(doc.getId())
				.name(doc.getName())
				.color(doc.getColor())
				.enabled(doc.isEnabled())
				.createdBy(doc.getCreatedBy())
				.build();
	}
}
