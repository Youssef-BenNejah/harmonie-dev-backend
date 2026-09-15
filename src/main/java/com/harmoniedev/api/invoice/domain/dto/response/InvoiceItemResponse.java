package com.harmoniedev.api.invoice.domain.dto.response;

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
public class InvoiceItemResponse {
	private String id;
	private String ref;
	private String article;
	private String description;
	private double quantity;
	private double price;
	private String taxId;
	private double taxRate;
	private double taxAmount;
	private String taxName;
	private double total;
}
