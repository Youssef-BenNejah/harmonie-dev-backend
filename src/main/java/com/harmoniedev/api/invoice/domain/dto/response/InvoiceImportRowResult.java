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
public class InvoiceImportRowResult {
	private int line;
	private boolean success;
	/** Explains why the row failed; null on success. */
	private String message;
	private String invoiceId;
}
