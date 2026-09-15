package com.harmoniedev.api.catalog.repository;

import com.harmoniedev.api.catalog.domain.model.ProductCategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductCategoryRepository extends MongoRepository<ProductCategoryDocument, String> {
}
