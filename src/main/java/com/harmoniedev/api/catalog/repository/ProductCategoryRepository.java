package com.harmoniedev.api.catalog.repository;

import com.harmoniedev.api.catalog.domain.model.ProductCategoryDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductCategoryRepository extends MongoRepository<ProductCategoryDocument, String> {
	List<ProductCategoryDocument> findByCreatedBy(String createdBy);
}
