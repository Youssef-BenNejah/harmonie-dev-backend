package com.harmoniedev.api.client.domain.dto.response;

import com.harmoniedev.api.client.domain.enums.ClientType;
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
public class ClientResponse {
	private String id;
	private ClientType type;
	private PersonSnapshotResponse person;
	private EntrepriseSnapshotResponse entreprise;
	private String createdBy;
	private Instant created;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class PersonSnapshotResponse {
		private String id;
		private String prenom;
		private String nom;
		private String email;
		private String telephone;
		private String cin;
		private String adresse;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class EntrepriseSnapshotResponse {
		private String id;
		private String nom;
		private String email;
		private String telephone;
		private String fisc;
		private String adresse;
	}
}
