package com.harmoniedev.api.plan.domain.dto.response;

import java.util.List;
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
public class PlanResponse {
	private String id;
	private String nom;
	private String tagline;
	private double prixMensuel;
	private double prixAnnuel;
	private boolean populaire;
	private List<String> fonctionnalites;
}
