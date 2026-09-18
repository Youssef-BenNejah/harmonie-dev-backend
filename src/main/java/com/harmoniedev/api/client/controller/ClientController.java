package com.harmoniedev.api.client.controller;

import com.harmoniedev.api.client.domain.dto.request.CreateClientRequest;
import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.client.service.ClientService;
import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.common.paging.Paging;
import com.harmoniedev.api.security.AuthenticatedUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ClientResponse>> create(
			@RequestBody CreateClientRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		ClientResponse created = clientService.create(request.getType(), request.getPersonId(), request.getEntrepriseId(), actorId);
		return ResponseEntity.ok(ApiResponse.success("Client created", created));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ClientResponse>>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return Paging.respond("Clients", clientService.listPage(Paging.of(page, size)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ClientResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Client", clientService.get(id)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		clientService.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Client deleted", null));
	}
}
