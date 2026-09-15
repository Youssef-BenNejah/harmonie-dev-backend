package com.harmoniedev.api.person.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Used for both create (all fields required) and update (server ignores absent fields). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonRequest {
	@NotBlank
	private String prenom;

	@NotBlank
	private String nom;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String telephone;

	@NotBlank
	private String pays;

	private String cin;

	@NotBlank
	private String adresse;

	private String entrepriseId;
}
