package com.harmoniedev.api.person.repository;

import com.harmoniedev.api.person.domain.model.PersonDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PersonRepository extends MongoRepository<PersonDocument, String> {
	List<PersonDocument> findByCreatedBy(String createdBy);

	Page<PersonDocument> findByCreatedBy(String createdBy, Pageable pageable);
}
