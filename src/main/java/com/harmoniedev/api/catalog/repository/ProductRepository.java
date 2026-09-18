package com.harmoniedev.api.catalog.repository;

import com.harmoniedev.api.catalog.domain.model.ProductDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<ProductDocument, String> {
	List<ProductDocument> findByCreatedBy(String createdBy);

	Page<ProductDocument> findByCreatedBy(String createdBy, Pageable pageable);
}
