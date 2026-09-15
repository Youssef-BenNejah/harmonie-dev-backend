package com.harmoniedev.api.tax.repository;

import com.harmoniedev.api.tax.domain.model.TaxDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaxRepository extends MongoRepository<TaxDocument, String> {
}
