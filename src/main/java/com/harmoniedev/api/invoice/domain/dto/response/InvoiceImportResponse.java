package com.harmoniedev.api.invoice.domain.dto.response;

import java.util.List;
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
public class InvoiceImportResponse {
	private int imported;
	private int failed;
	private List<InvoiceImportRowResult> results;
}
