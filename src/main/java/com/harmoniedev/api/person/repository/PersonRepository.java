package com.harmoniedev.api.person.repository;

import com.harmoniedev.api.person.domain.model.PersonDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PersonRepository extends MongoRepository<PersonDocument, String> {
}
