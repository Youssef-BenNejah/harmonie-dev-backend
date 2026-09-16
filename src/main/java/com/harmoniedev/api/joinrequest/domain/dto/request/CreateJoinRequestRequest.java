package com.harmoniedev.api.joinrequest.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class CreateJoinRequestRequest {
	@NotBlank
	private String nom;

	@NotBlank
	private String prenom;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String telephone;

	@NotBlank
	private String entreprise;

	private String message;

	/** id of the Plan the prospect selected on the landing page's pricing section. */
	private String requestedPlanId;
}
