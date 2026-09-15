package com.harmoniedev.api.entreprise.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.entreprise.domain.dto.request.EntrepriseRequest;
import com.harmoniedev.api.entreprise.domain.dto.response.EntrepriseResponse;
import com.harmoniedev.api.entreprise.service.EntrepriseService;
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
@RequestMapping("/api/v1/entreprises")
public class EntrepriseController {
	private final EntrepriseService entrepriseService;

	public EntrepriseController(EntrepriseService entrepriseService) {
		this.entrepriseService = entrepriseService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<EntrepriseResponse>> create(
			@Valid @RequestBody EntrepriseRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Entreprise created", entrepriseService.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<EntrepriseResponse>>> list() {
		return ResponseEntity.ok(ApiResponse.success("Entreprises", entrepriseService.list()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<EntrepriseResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Entreprise", entrepriseService.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<EntrepriseResponse>> update(
			@PathVariable String id, @Valid @RequestBody EntrepriseRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Entreprise updated", entrepriseService.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		entrepriseService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Entreprise deleted", null));
	}
}
