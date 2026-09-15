package com.harmoniedev.api.joinrequest.repository;

import com.harmoniedev.api.joinrequest.domain.model.JoinRequestDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JoinRequestRepository extends MongoRepository<JoinRequestDocument, String> {
}
