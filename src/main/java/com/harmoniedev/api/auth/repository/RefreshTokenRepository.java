package com.harmoniedev.api.auth.repository;

import com.harmoniedev.api.auth.domain.model.RefreshTokenDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefreshTokenRepository extends MongoRepository<RefreshTokenDocument, String> {
	List<RefreshTokenDocument> findByUserIdAndRevokedFalse(String userId);
	Optional<RefreshTokenDocument> findByTokenHash(String tokenHash);
}