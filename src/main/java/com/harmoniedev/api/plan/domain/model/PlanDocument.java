package com.harmoniedev.api.plan.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDocument {
	@Id
	private String id;
	private String nom;
	private String tagline;
	private double prixMensuel;
	private double prixAnnuel;
	@Default
	private boolean populaire = false;
	@Default
	private List<String> fonctionnalites = List.of();
}
