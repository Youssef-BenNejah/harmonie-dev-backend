package com.harmoniedev.api.entreprise.domain.model;

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

@Document(collection = "entreprises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntrepriseDocument {
	@Id
	private String id;
	private String nom;
	private String email;
	private String telephone;
	private String pays;
	private String siteweb;
	private String rib;
	private String fisc;
	private String adresse;
	@Default
	private boolean isClient = false;
	private MainContactRef mainContact;
	private String createdBy;
	@CreatedDate
	private Instant created;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class MainContactRef {
		private String id;
		private String prenom;
		private String nom;
	}
}
