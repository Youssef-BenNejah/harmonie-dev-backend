package com.harmoniedev.api.entreprise.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.entreprise.domain.dto.request.EntrepriseRequest;
import com.harmoniedev.api.entreprise.domain.dto.response.EntrepriseResponse;
import com.harmoniedev.api.entreprise.domain.model.EntrepriseDocument;
import com.harmoniedev.api.entreprise.repository.EntrepriseRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EntrepriseService {
	private final EntrepriseRepository repository;

	public EntrepriseService(EntrepriseRepository repository) {
		this.repository = repository;
	}

	public EntrepriseResponse create(EntrepriseRequest request, String actorId) {
		EntrepriseDocument doc = EntrepriseDocument.builder()
				.nom(request.getNom())
				.email(request.getEmail())
				.telephone(request.getTelephone())
				.pays(request.getPays())
				.siteweb(request.getSiteweb())
				.rib(request.getRib())
				.fisc(request.getFisc())
				.adresse(request.getAdresse())
				.isClient(false)
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<EntrepriseResponse> list() {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public EntrepriseResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public EntrepriseResponse update(String id, EntrepriseRequest request) {
		EntrepriseDocument doc = findOrThrow(id);
		doc.setNom(request.getNom());
		doc.setEmail(request.getEmail());
		doc.setTelephone(request.getTelephone());
		doc.setPays(request.getPays());
		doc.setSiteweb(request.getSiteweb());
		doc.setRib(request.getRib());
		doc.setFisc(request.getFisc());
		doc.setAdresse(request.getAdresse());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		repository.delete(findOrThrow(id));
	}

	EntrepriseDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(d -> TenantScope.owns(d.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise not found"));
	}

	EntrepriseResponse toResponse(EntrepriseDocument doc) {
		return EntrepriseResponse.builder()
				.id(doc.getId())
				.nom(doc.getNom())
				.email(doc.getEmail())
				.telephone(doc.getTelephone())
				.pays(doc.getPays())
				.siteweb(doc.getSiteweb())
				.rib(doc.getRib())
				.fisc(doc.getFisc())
				.adresse(doc.getAdresse())
				.client(doc.isClient())
				.mainContact(doc.getMainContact() == null ? null
						: EntrepriseResponse.MainContactRefResponse.builder()
								.id(doc.getMainContact().getId())
								.prenom(doc.getMainContact().getPrenom())
								.nom(doc.getMainContact().getNom())
								.build())
				.createdBy(doc.getCreatedBy())
				.created(doc.getCreated())
				.build();
	}
}
