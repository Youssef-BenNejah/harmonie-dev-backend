package com.harmoniedev.api.entreprise.repository;

import com.harmoniedev.api.entreprise.domain.model.EntrepriseDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EntrepriseRepository extends MongoRepository<EntrepriseDocument, String> {
	List<EntrepriseDocument> findByCreatedBy(String createdBy);

	Page<EntrepriseDocument> findByCreatedBy(String createdBy, Pageable pageable);
}
