package com.harmoniedev.api.entreprise.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
	@Size(max = 255)
	private String nom;

	@NotBlank
	@Email
	@Size(max = 255)
	private String email;

	@NotBlank
	@Size(max = 255)
	private String telephone;

	@NotBlank
	@Size(max = 255)
	private String pays;

	@Size(max = 255)
	private String siteweb;

	@Size(max = 255)
	private String rib;

	@NotBlank
	@Size(max = 255)
	private String fisc;

	@NotBlank
	@Size(max = 255)
	private String adresse;

	private String mainContactId;
}
