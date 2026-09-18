package com.harmoniedev.api.person.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
	@Size(max = 255)
	private String prenom;

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
	private String cin;

	@NotBlank
	@Size(max = 255)
	private String adresse;

	private String entrepriseId;
}
