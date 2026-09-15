package com.harmoniedev.api.joinrequest.domain.model;

import com.harmoniedev.api.joinrequest.domain.enums.JoinRequestStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRequestDocument {
	@Id
	private String id;
	private String nom;
	private String prenom;
	private String email;
	private String telephone;
	private String entreprise;
	private String message;
	@Default
	private JoinRequestStatus status = JoinRequestStatus.PENDING;
	@CreatedDate
	private Instant createdAt;
}
