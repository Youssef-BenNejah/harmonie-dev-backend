package com.harmoniedev.api.plan.repository;

import com.harmoniedev.api.plan.domain.model.PlanDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlanRepository extends MongoRepository<PlanDocument, String> {
}
