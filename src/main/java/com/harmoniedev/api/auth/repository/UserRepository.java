package com.harmoniedev.api.auth.repository;

import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserDocument, String> {
	Optional<UserDocument> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByRole(Role role);
	List<UserDocument> findByRole(Role role);
}
