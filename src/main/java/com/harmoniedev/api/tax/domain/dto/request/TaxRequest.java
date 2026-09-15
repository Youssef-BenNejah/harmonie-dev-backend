package com.harmoniedev.api.tax.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
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
public class TaxRequest {
	@NotBlank
	private String name;

	private double taxvalue;

	@JsonProperty("isActive")
	private boolean active;

	@JsonProperty("isDefault")
	private boolean defaultTax;
}
