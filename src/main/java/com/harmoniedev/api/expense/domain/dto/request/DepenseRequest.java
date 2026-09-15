package com.harmoniedev.api.expense.domain.dto.request;

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
public class DepenseRequest {
	@NotBlank
	private String name;

	@NotBlank
	private String currency;

	private double price;

	private String description;

	private String reference;

	private String categoryId;
}
