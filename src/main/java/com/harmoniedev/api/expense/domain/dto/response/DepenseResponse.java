package com.harmoniedev.api.expense.domain.dto.response;

import java.time.Instant;
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
public class DepenseResponse {
	private String id;
	private String name;
	private String currency;
	private double price;
	private String description;
	private String reference;
	private String categoryId;
	private String createdBy;
	private Instant created;
}
