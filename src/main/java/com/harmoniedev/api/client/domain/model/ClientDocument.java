package com.harmoniedev.api.client.domain.model;

import com.harmoniedev.api.client.domain.enums.ClientType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDocument {
	@Id
	private String id;
	private ClientType type;
	private PersonSnapshot person;
	private EntrepriseSnapshot entreprise;
	private String createdBy;
	@CreatedDate
	private Instant created;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class PersonSnapshot {
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
	public static class EntrepriseSnapshot {
		private String id;
		private String nom;
		private String email;
		private String telephone;
		private String fisc;
		private String adresse;
	}
}
