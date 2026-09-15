package com.harmoniedev.api.entreprise.repository;

import com.harmoniedev.api.entreprise.domain.model.EntrepriseDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EntrepriseRepository extends MongoRepository<EntrepriseDocument, String> {
}
