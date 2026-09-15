package com.harmoniedev.api.entreprise.domain.dto.response;

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
public class EntrepriseResponse {
	private String id;
	private String nom;
	private String email;
	private String telephone;
	private String pays;
	private String siteweb;
	private String rib;
	private String fisc;
	private String adresse;
	@JsonProperty("isClient")
	private boolean client;
	private MainContactRefResponse mainContact;
	private String createdBy;
	private Instant created;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class MainContactRefResponse {
		private String id;
		private String prenom;
		private String nom;
	}
}
