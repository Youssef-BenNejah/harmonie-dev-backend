package com.harmoniedev.api.client.service;

import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.client.domain.enums.ClientType;
import com.harmoniedev.api.client.domain.model.ClientDocument;
import com.harmoniedev.api.client.repository.ClientRepository;
import com.harmoniedev.api.entreprise.domain.model.EntrepriseDocument;
import com.harmoniedev.api.entreprise.repository.EntrepriseRepository;
import com.harmoniedev.api.person.domain.model.PersonDocument;
import com.harmoniedev.api.person.repository.PersonRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClientService {
	private final ClientRepository clientRepository;
	private final PersonRepository personRepository;
	private final EntrepriseRepository entrepriseRepository;

	public ClientService(
			ClientRepository clientRepository,
			PersonRepository personRepository,
			EntrepriseRepository entrepriseRepository) {
		this.clientRepository = clientRepository;
		this.personRepository = personRepository;
		this.entrepriseRepository = entrepriseRepository;
	}

	/** Flags the Person as a client and creates the wrapping Client record from a snapshot of it. */
	public ClientResponse convertPerson(String personId, String actorId) {
		PersonDocument person = personRepository.findById(personId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));
		person.setClient(true);
		personRepository.save(person);
		ClientDocument doc = ClientDocument.builder()
				.type(ClientType.PERSON)
				.person(ClientDocument.PersonSnapshot.builder()
						.id(person.getId())
						.prenom(person.getPrenom())
						.nom(person.getNom())
						.email(person.getEmail())
						.telephone(person.getTelephone())
						.cin(person.getCin())
						.adresse(person.getAdresse())
						.build())
				.createdBy(actorId)
				.build();
		clientRepository.save(doc);
		return toResponse(doc);
	}

	/** Flags the Entreprise as a client and creates the wrapping Client record from a snapshot of it. */
	public ClientResponse convertEntreprise(String entrepriseId, String actorId) {
		EntrepriseDocument entreprise = entrepriseRepository.findById(entrepriseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise not found"));
		entreprise.setClient(true);
		entrepriseRepository.save(entreprise);
		ClientDocument doc = ClientDocument.builder()
				.type(ClientType.COMPANY)
				.entreprise(ClientDocument.EntrepriseSnapshot.builder()
						.id(entreprise.getId())
						.nom(entreprise.getNom())
						.email(entreprise.getEmail())
						.telephone(entreprise.getTelephone())
						.fisc(entreprise.getFisc())
						.adresse(entreprise.getAdresse())
						.build())
				.createdBy(actorId)
				.build();
		clientRepository.save(doc);
		return toResponse(doc);
	}

	/** Generic entry point behind `POST /clients` — creates from an existing Person or Entreprise. */
	public ClientResponse create(ClientType type, String personId, String entrepriseId, String actorId) {
		if (type == ClientType.PERSON) {
			if (personId == null || personId.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personId is required for type=PERSON");
			}
			return convertPerson(personId, actorId);
		}
		if (entrepriseId == null || entrepriseId.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entrepriseId is required for type=COMPANY");
		}
		return convertEntreprise(entrepriseId, actorId);
	}

	public List<ClientResponse> list() {
		return clientRepository.findAll().stream().map(this::toResponse).toList();
	}

	public ClientResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public void delete(String id) {
		if (!clientRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
		}
		clientRepository.deleteById(id);
	}

	private ClientDocument findOrThrow(String id) {
		return clientRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
	}

	private ClientResponse toResponse(ClientDocument doc) {
		return ClientResponse.builder()
				.id(doc.getId())
				.type(doc.getType())
				.person(doc.getPerson() == null ? null
						: ClientResponse.PersonSnapshotResponse.builder()
								.id(doc.getPerson().getId())
								.prenom(doc.getPerson().getPrenom())
								.nom(doc.getPerson().getNom())
								.email(doc.getPerson().getEmail())
								.telephone(doc.getPerson().getTelephone())
								.cin(doc.getPerson().getCin())
								.adresse(doc.getPerson().getAdresse())
								.build())
				.entreprise(doc.getEntreprise() == null ? null
						: ClientResponse.EntrepriseSnapshotResponse.builder()
								.id(doc.getEntreprise().getId())
								.nom(doc.getEntreprise().getNom())
								.email(doc.getEntreprise().getEmail())
								.telephone(doc.getEntreprise().getTelephone())
								.fisc(doc.getEntreprise().getFisc())
								.adresse(doc.getEntreprise().getAdresse())
								.build())
				.createdBy(doc.getCreatedBy())
				.created(doc.getCreated())
				.build();
	}
}
