package com.harmoniedev.api.catalog.repository;

import com.harmoniedev.api.catalog.domain.model.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<ProductDocument, String> {
}
