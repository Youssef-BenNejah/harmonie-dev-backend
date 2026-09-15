package com.harmoniedev.api.catalog.domain.dto.request;

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
public class ProductCategoryRequest {
	@NotBlank
	private String name;

	private String description;

	@NotBlank
	private String color;

	private boolean enabled;
}
