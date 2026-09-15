package com.harmoniedev.api.expense.repository;

import com.harmoniedev.api.expense.domain.model.DepenseCategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepenseCategoryRepository extends MongoRepository<DepenseCategoryDocument, String> {
}
