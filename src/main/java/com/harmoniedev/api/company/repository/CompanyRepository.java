package com.harmoniedev.api.company.repository;

import com.harmoniedev.api.company.domain.model.CompanyDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyRepository extends MongoRepository<CompanyDocument, String> {
	Optional<CompanyDocument> findByOwnerId(String ownerId);
}
