package com.harmoniedev.api.tax.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "taxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDocument {
	@Id
	private String id;
	private String name;
	private double taxvalue;
	@Default
	private boolean isActive = true;
	@Default
	private boolean isDefault = false;
	private String createdBy;
}
