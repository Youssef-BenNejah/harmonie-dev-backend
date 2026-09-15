package com.harmoniedev.api.person.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PersonResponse {
	private String id;
	private String prenom;
	private String nom;
	private String email;
	private String telephone;
	private String pays;
	private String cin;
	private String adresse;
	@JsonProperty("isClient")
	private boolean client;
	private EntrepriseRefResponse entreprise;
	private String createdBy;
	private Instant created;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class EntrepriseRefResponse {
		private String id;
		private String nom;
	}
}
