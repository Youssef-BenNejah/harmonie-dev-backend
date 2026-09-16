package com.harmoniedev.api.currency.repository;

import com.harmoniedev.api.currency.domain.model.CurrencyDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CurrencyRepository extends MongoRepository<CurrencyDocument, String> {
	List<CurrencyDocument> findByCreatedBy(String createdBy);
}
