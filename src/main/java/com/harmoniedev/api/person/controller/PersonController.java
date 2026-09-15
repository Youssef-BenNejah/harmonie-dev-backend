package com.harmoniedev.api.person.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.person.domain.dto.request.PersonRequest;
import com.harmoniedev.api.person.domain.dto.response.PersonResponse;
import com.harmoniedev.api.person.service.PersonService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/persons")
public class PersonController {
	private final PersonService personService;

	public PersonController(PersonService personService) {
		this.personService = personService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PersonResponse>> create(
			@Valid @RequestBody PersonRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Person created", personService.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PersonResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Persons", personService.list()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PersonResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Person", personService.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<PersonResponse>> update(
			@PathVariable String id, @Valid @RequestBody PersonRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Person updated", personService.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		personService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Person deleted", null));
	}
}
