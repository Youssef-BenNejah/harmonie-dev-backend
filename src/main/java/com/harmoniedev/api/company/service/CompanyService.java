package com.harmoniedev.api.company.service;

import com.harmoniedev.api.company.domain.dto.request.CompanyRequest;
import com.harmoniedev.api.company.domain.dto.response.CompanyResponse;
import com.harmoniedev.api.company.domain.model.CompanyDocument;
import com.harmoniedev.api.company.repository.CompanyRepository;
import com.harmoniedev.api.storage.FileStorageService;
import com.harmoniedev.api.storage.CloudinaryUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CompanyService {
	private final CompanyRepository repository;
	private final FileStorageService cloudinaryService;

	public CompanyService(CompanyRepository repository, FileStorageService cloudinaryService) {
		this.repository = repository;
		this.cloudinaryService = cloudinaryService;
	}

	public CompanyResponse getOrCreate(String ownerId) {
		return toResponse(findOrCreate(ownerId));
	}

	public CompanyResponse update(String ownerId, CompanyRequest request) {
		CompanyDocument doc = findOrCreate(ownerId);
		doc.setName(request.getName());
		doc.setMatriculeFisc(request.getMatriculeFisc());
		doc.setAddress(request.getAddress());
		doc.setState(request.getState());
		doc.setCountry(request.getCountry());
		doc.setEmail(request.getEmail());
		doc.setPhone(request.getPhone());
		doc.setWebsite(request.getWebsite());
		doc.setTaxNumber(request.getTaxNumber());
		doc.setVatNumber(request.getVatNumber());
		doc.setRegistrationNumber(request.getRegistrationNumber());
		repository.save(doc);
		return toResponse(doc);
	}

	public CompanyResponse uploadLogo(String ownerId, MultipartFile file) {
		CompanyDocument doc = findOrCreate(ownerId);
		CloudinaryUploadResult result = cloudinaryService.uploadImage(file, "companies/" + ownerId, "logo");
		doc.setLogoUrl(result.url());
		doc.setLogoPublicId(result.publicId());
		repository.save(doc);
		return toResponse(doc);
	}

	private CompanyDocument findOrCreate(String ownerId) {
		return repository.findByOwnerId(ownerId).orElseGet(() -> {
			CompanyDocument doc = CompanyDocument.builder().ownerId(ownerId).name("").build();
			return repository.save(doc);
		});
	}

	private CompanyResponse toResponse(CompanyDocument doc) {
		return CompanyResponse.builder()
				.id(doc.getId())
				.name(doc.getName())
				.matriculeFisc(doc.getMatriculeFisc())
				.address(doc.getAddress())
				.state(doc.getState())
				.country(doc.getCountry())
				.email(doc.getEmail())
				.phone(doc.getPhone())
				.website(doc.getWebsite())
				.taxNumber(doc.getTaxNumber())
				.vatNumber(doc.getVatNumber())
				.registrationNumber(doc.getRegistrationNumber())
				.logoUrl(doc.getLogoUrl())
				.build();
	}
}
