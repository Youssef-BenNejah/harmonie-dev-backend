package com.harmoniedev.api.plan.service;

import com.harmoniedev.api.plan.domain.dto.request.PlanRequest;
import com.harmoniedev.api.plan.domain.dto.response.PlanResponse;
import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanService {
	private final PlanRepository repository;

	public PlanService(PlanRepository repository) {
		this.repository = repository;
	}

	public List<PlanResponse> list() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	public PlanResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public PlanResponse create(PlanRequest request) {
		if (request.isFreeTrial()) {
			clearExistingFreeTrial();
		}
		PlanDocument doc = PlanDocument.builder()
				.nom(request.getNom())
				.tagline(request.getTagline())
				.prixMensuel(request.getPrixMensuel())
				.prixAnnuel(request.getPrixAnnuel())
				.populaire(request.isPopulaire())
				.fonctionnalites(request.getFonctionnalites() != null ? request.getFonctionnalites() : List.of())
				.isFreeTrial(request.isFreeTrial())
				.trialDurationDays(request.getTrialDurationDays())
				.maxInvoicesPerMonth(request.getMaxInvoicesPerMonth())
				.maxClients(request.getMaxClients())
				.maxProducts(request.getMaxProducts())
				.maxCustomTaxes(request.getMaxCustomTaxes())
				.multiCurrency(request.isMultiCurrency())
				.reportsAccess(request.isReportsAccess())
				.expensesEnabled(request.isExpensesEnabled())
				.bulkExportEnabled(request.isBulkExportEnabled())
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public PlanResponse update(String id, PlanRequest request) {
		PlanDocument doc = findOrThrow(id);
		if (request.isFreeTrial() && !doc.isFreeTrial()) {
			clearExistingFreeTrial();
		}
		doc.setNom(request.getNom());
		doc.setTagline(request.getTagline());
		doc.setPrixMensuel(request.getPrixMensuel());
		doc.setPrixAnnuel(request.getPrixAnnuel());
		doc.setPopulaire(request.isPopulaire());
		doc.setFonctionnalites(request.getFonctionnalites() != null ? request.getFonctionnalites() : List.of());
		doc.setFreeTrial(request.isFreeTrial());
		doc.setTrialDurationDays(request.getTrialDurationDays());
		doc.setMaxInvoicesPerMonth(request.getMaxInvoicesPerMonth());
		doc.setMaxClients(request.getMaxClients());
		doc.setMaxProducts(request.getMaxProducts());
		doc.setMaxCustomTaxes(request.getMaxCustomTaxes());
		doc.setMultiCurrency(request.isMultiCurrency());
		doc.setReportsAccess(request.isReportsAccess());
		doc.setExpensesEnabled(request.isExpensesEnabled());
		doc.setBulkExportEnabled(request.isBulkExportEnabled());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
		}
		repository.deleteById(id);
	}

	private void clearExistingFreeTrial() {
		repository.findAll().stream()
				.filter(PlanDocument::isFreeTrial)
				.forEach(p -> {
					p.setFreeTrial(false);
					repository.save(p);
				});
	}

	private PlanDocument findOrThrow(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
	}

	private PlanResponse toResponse(PlanDocument doc) {
		return PlanResponse.builder()
				.id(doc.getId())
				.nom(doc.getNom())
				.tagline(doc.getTagline())
				.prixMensuel(doc.getPrixMensuel())
				.prixAnnuel(doc.getPrixAnnuel())
				.populaire(doc.isPopulaire())
				.fonctionnalites(doc.getFonctionnalites())
				.isFreeTrial(doc.isFreeTrial())
				.trialDurationDays(doc.getTrialDurationDays())
				.maxInvoicesPerMonth(doc.getMaxInvoicesPerMonth())
				.maxClients(doc.getMaxClients())
				.maxProducts(doc.getMaxProducts())
				.maxCustomTaxes(doc.getMaxCustomTaxes())
				.multiCurrency(doc.isMultiCurrency())
				.reportsAccess(doc.isReportsAccess())
				.expensesEnabled(doc.isExpensesEnabled())
				.bulkExportEnabled(doc.isBulkExportEnabled())
				.build();
	}
}
