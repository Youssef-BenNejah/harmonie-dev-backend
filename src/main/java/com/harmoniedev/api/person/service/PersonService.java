package com.harmoniedev.api.person.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.person.domain.dto.request.PersonRequest;
import com.harmoniedev.api.person.domain.dto.response.PersonResponse;
import com.harmoniedev.api.person.domain.model.PersonDocument;
import com.harmoniedev.api.person.repository.PersonRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonService {
	private final PersonRepository repository;

	public PersonService(PersonRepository repository) {
		this.repository = repository;
	}

	public PersonResponse create(PersonRequest request, String actorId) {
		PersonDocument doc = PersonDocument.builder()
				.prenom(request.getPrenom())
				.nom(request.getNom())
				.email(request.getEmail())
				.telephone(request.getTelephone())
				.pays(request.getPays())
				.cin(request.getCin())
				.adresse(request.getAdresse())
				.isClient(false)
				.createdBy(actorId)
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	public List<PersonResponse> list() {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public Page<PersonResponse> listPage(Pageable pageable) {
		return repository.findByCreatedBy(TenantScope.currentId(), pageable).map(this::toResponse);
	}

	public PersonResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public PersonResponse update(String id, PersonRequest request) {
		PersonDocument doc = findOrThrow(id);
		doc.setPrenom(request.getPrenom());
		doc.setNom(request.getNom());
		doc.setEmail(request.getEmail());
		doc.setTelephone(request.getTelephone());
		doc.setPays(request.getPays());
		doc.setCin(request.getCin());
		doc.setAdresse(request.getAdresse());
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		repository.delete(findOrThrow(id));
	}

	PersonDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(d -> TenantScope.owns(d.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
	}

	PersonResponse toResponse(PersonDocument doc) {
		return PersonResponse.builder()
				.id(doc.getId())
				.prenom(doc.getPrenom())
				.nom(doc.getNom())
				.email(doc.getEmail())
				.telephone(doc.getTelephone())
				.pays(doc.getPays())
				.cin(doc.getCin())
				.adresse(doc.getAdresse())
				.client(doc.isClient())
				.entreprise(doc.getEntreprise() == null ? null
						: PersonResponse.EntrepriseRefResponse.builder()
								.id(doc.getEntreprise().getId())
								.nom(doc.getEntreprise().getNom())
								.build())
				.createdBy(doc.getCreatedBy())
				.created(doc.getCreated())
				.build();
	}
}
