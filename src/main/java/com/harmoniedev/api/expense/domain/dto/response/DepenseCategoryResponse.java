package com.harmoniedev.api.expense.domain.dto.response;

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
public class DepenseCategoryResponse {
	private String id;
	private String name;
	private String color;
	private boolean enabled;
	private String createdBy;
}
