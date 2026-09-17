package com.harmoniedev.api.client.repository;

import com.harmoniedev.api.client.domain.model.ClientDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientRepository extends MongoRepository<ClientDocument, String> {
	List<ClientDocument> findByCreatedBy(String createdBy);
}
