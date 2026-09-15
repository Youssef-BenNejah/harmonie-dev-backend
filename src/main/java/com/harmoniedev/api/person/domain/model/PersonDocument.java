package com.harmoniedev.api.person.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDocument {
	@Id
	private String id;
	private String prenom;
	private String nom;
	private String email;
	private String telephone;
	private String pays;
	private String cin;
	private String adresse;
	@Default
	private boolean isClient = false;
	private EntrepriseRef entreprise;
	private String createdBy;
	@CreatedDate
	private Instant created;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class EntrepriseRef {
		private String id;
		private String nom;
	}
}
