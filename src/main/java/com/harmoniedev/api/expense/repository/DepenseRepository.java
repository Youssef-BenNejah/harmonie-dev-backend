package com.harmoniedev.api.expense.repository;

import com.harmoniedev.api.expense.domain.model.DepenseDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepenseRepository extends MongoRepository<DepenseDocument, String> {
	List<DepenseDocument> findByCreatedBy(String createdBy);
}
