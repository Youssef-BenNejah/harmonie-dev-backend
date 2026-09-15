package com.harmoniedev.api.currency.repository;

import com.harmoniedev.api.currency.domain.model.CurrencyDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CurrencyRepository extends MongoRepository<CurrencyDocument, String> {
}
