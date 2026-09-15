package com.harmoniedev.api.invoice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
public class InvoiceItemRequest {
	private String ref;

	@NotBlank
	private String article;

	private String description;

	@Positive
	private double quantity;

	private double price;

	/** Empty/null means no tax on this line. */
	private String taxId;
}
