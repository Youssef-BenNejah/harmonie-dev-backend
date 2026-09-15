package com.harmoniedev.api.entreprise.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Used for both create (all required fields enforced) and update. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseRequest {
	@NotBlank
	private String nom;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String telephone;

	@NotBlank
	private String pays;

	private String siteweb;

	private String rib;

	@NotBlank
	private String fisc;

	@NotBlank
	private String adresse;

	private String mainContactId;
}
