package com.harmoniedev.api.company.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.company.domain.dto.request.CompanyRequest;
import com.harmoniedev.api.company.domain.dto.response.CompanyResponse;
import com.harmoniedev.api.company.service.CompanyService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/company")
public class CompanyController {
	private final CompanyService service;

	public CompanyController(CompanyService service) {
		this.service = service;
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<CompanyResponse>> me(Authentication authentication) {
		String ownerId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Company", service.getOrCreate(ownerId)));
	}

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<CompanyResponse>> update(
			@Valid @RequestBody CompanyRequest request, Authentication authentication) {
		String ownerId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Company updated", service.update(ownerId, request)));
	}

	@PostMapping("/me/logo")
	public ResponseEntity<ApiResponse<CompanyResponse>> uploadLogo(
			@RequestParam("file") MultipartFile file, Authentication authentication) {
		String ownerId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Logo uploaded", service.uploadLogo(ownerId, file)));
	}
}
