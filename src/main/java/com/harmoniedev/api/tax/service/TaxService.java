package com.harmoniedev.api.tax.service;

import com.harmoniedev.api.tax.domain.dto.request.TaxRequest;
import com.harmoniedev.api.tax.domain.dto.response.TaxResponse;
import com.harmoniedev.api.tax.domain.model.TaxDocument;
import com.harmoniedev.api.plan.service.PlanUsageService;
import com.harmoniedev.api.tax.repository.TaxRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaxService {
	private final TaxRepository repository;
	private final PlanUsageService planUsageService;

	public TaxService(TaxRepository repository, PlanUsageService planUsageService) {
		this.repository = repository;
		this.planUsageService = planUsageService;
	}

	public TaxResponse create(TaxRequest request, String actorId) {
		planUsageService.assertCanCreateCustomTax(actorId, request.isDefaultTax());
		if (request.isDefaultTax()) {
			clearExistingDefault();
		}
		TaxDocument doc = TaxDocument.builder()
				.name(request.getName())
				.taxvalue(request.getTaxvalue())
				.isActive(request.isActive())
				.isDefault(request.isDefaultTax())
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<TaxResponse> list() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	public TaxResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public TaxResponse update(String id, TaxRequest request) {
		TaxDocument doc = findOrThrow(id);
		if (request.isDefaultTax() && !doc.isDefault()) {
			clearExistingDefault();
		}
		doc.setName(request.getName());
		doc.setTaxvalue(request.getTaxvalue());
		doc.setActive(request.isActive());
		doc.setDefault(request.isDefaultTax());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax not found");
		}
		repository.deleteById(id);
	}

	public TaxResponse setDefault(String id) {
		TaxDocument doc = findOrThrow(id);
		clearExistingDefault();
		doc.setDefault(true);
		repository.save(doc);
		return toResponse(doc);
	}

	private void clearExistingDefault() {
		repository.findAll().stream()
				.filter(TaxDocument::isDefault)
				.forEach(t -> {
					t.setDefault(false);
					repository.save(t);
				});
	}

	private TaxDocument findOrThrow(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax not found"));
	}

	private TaxResponse toResponse(TaxDocument doc) {
		return TaxResponse.builder()
				.id(doc.getId())
				.name(doc.getName())
				.taxvalue(doc.getTaxvalue())
				.active(doc.isActive())
				.defaultTax(doc.isDefault())
				.createdBy(doc.getCreatedBy())
				.build();
	}
}
