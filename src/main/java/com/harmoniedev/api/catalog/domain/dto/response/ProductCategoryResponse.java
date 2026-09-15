package com.harmoniedev.api.catalog.domain.dto.response;

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
public class ProductCategoryResponse {
	private String id;
	private String name;
	private String description;
	private String color;
	private boolean enabled;
	private String createdBy;
}
