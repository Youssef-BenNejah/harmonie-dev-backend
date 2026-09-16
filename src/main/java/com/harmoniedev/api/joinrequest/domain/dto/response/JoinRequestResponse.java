package com.harmoniedev.api.joinrequest.domain.dto.response;

import com.harmoniedev.api.joinrequest.domain.enums.JoinRequestStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRequestResponse {
	private String id;
	private String nom;
	private String prenom;
	private String email;
	private String telephone;
	private String entreprise;
	private String message;
	private String requestedPlanId;
	private String requestedPlanNom;
	private JoinRequestStatus status;
	private Instant createdAt;
}
