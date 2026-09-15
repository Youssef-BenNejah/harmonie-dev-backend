package com.harmoniedev.api.tax.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TaxResponse {
	private String id;
	private String name;
	private double taxvalue;
	@JsonProperty("isActive")
	private boolean active;
	@JsonProperty("isDefault")
	private boolean defaultTax;
	private String createdBy;
}
