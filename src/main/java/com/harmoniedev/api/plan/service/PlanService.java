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
		PlanDocument doc = PlanDocument.builder()
				.nom(request.getNom())
				.tagline(request.getTagline())
				.prixMensuel(request.getPrixMensuel())
				.prixAnnuel(request.getPrixAnnuel())
				.populaire(request.isPopulaire())
				.fonctionnalites(request.getFonctionnalites() != null ? request.getFonctionnalites() : List.of())
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public PlanResponse update(String id, PlanRequest request) {
		PlanDocument doc = findOrThrow(id);
		doc.setNom(request.getNom());
		doc.setTagline(request.getTagline());
		doc.setPrixMensuel(request.getPrixMensuel());
		doc.setPrixAnnuel(request.getPrixAnnuel());
		doc.setPopulaire(request.isPopulaire());
		doc.setFonctionnalites(request.getFonctionnalites() != null ? request.getFonctionnalites() : List.of());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
		}
		repository.deleteById(id);
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
				.build();
	}
}
