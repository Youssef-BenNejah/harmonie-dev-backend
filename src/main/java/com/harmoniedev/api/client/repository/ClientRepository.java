package com.harmoniedev.api.client.repository;

import com.harmoniedev.api.client.domain.model.ClientDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientRepository extends MongoRepository<ClientDocument, String> {
	List<ClientDocument> findByCreatedBy(String createdBy);

	Page<ClientDocument> findByCreatedBy(String createdBy, Pageable pageable);
}
