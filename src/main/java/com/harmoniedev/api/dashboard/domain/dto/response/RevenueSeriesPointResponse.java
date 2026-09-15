package com.harmoniedev.api.dashboard.domain.dto.response;

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
public class RevenueSeriesPointResponse {
	private String mois;
	private double revenus;
	private double depenses;
}
