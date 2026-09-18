package com.harmoniedev.api.currency.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.currency.domain.dto.request.CurrencyRequest;
import com.harmoniedev.api.currency.domain.dto.response.CurrencyResponse;
import com.harmoniedev.api.currency.domain.model.CurrencyDocument;
import com.harmoniedev.api.currency.repository.CurrencyRepository;
import com.harmoniedev.api.plan.service.PlanUsageService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrencyService {
	private final CurrencyRepository repository;
	private final PlanUsageService planUsageService;

	public CurrencyService(CurrencyRepository repository, PlanUsageService planUsageService) {
		this.repository = repository;
		this.planUsageService = planUsageService;
	}

	public CurrencyResponse create(CurrencyRequest request, String actorId) {
		planUsageService.assertCanCreateCurrency(actorId);
		CurrencyDocument doc = CurrencyDocument.builder()
				.code(request.getCode().toUpperCase())
				.name(request.getName())
				.symbol(request.getSymbol())
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<CurrencyResponse> list(String actorId) {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public CurrencyResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public CurrencyResponse update(String id, CurrencyRequest request) {
		CurrencyDocument doc = findOrThrow(id);
		doc.setCode(request.getCode().toUpperCase());
		doc.setName(request.getName());
		doc.setSymbol(request.getSymbol());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		repository.delete(findOrThrow(id));
	}

	private CurrencyDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(d -> TenantScope.owns(d.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found"));
	}

	private CurrencyResponse toResponse(CurrencyDocument doc) {
		return CurrencyResponse.builder()
				.id(doc.getId())
				.code(doc.getCode())
				.name(doc.getName())
				.symbol(doc.getSymbol())
				.createdBy(doc.getCreatedBy())
				.build();
	}
}
